package dev.varshit.factory

import dev.varshit.helpers.StatusHelper
import dev.varshit.models.Resolution
import dev.varshit.models.Task
import dev.varshit.models.VideoRequestBody
import dev.varshit.plugins.Supabase
import dev.varshit.utilities.Helper.extractNumber
import dev.varshit.utilities.Helper.generateResolutionsList
import dev.varshit.utilities.Helper.getBandwidth
import dev.varshit.utilities.Helper.getResolution
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileFilter
import kotlin.time.Duration

object Status {
    const val STARTED = "Started"
    const val DOWNLOADING_FILE_LOCALLY = "Downloading File locally"
    const val TRANSCODING_VIDEO = "Transcoding video"
    const val UPLOADING_SEGMENTS = "Uploading segments"
    const val CREATING_PACKAGES = "Creating packages"
    const val UPLOADING_PACKAGES = "Uploading Packages"
    const val UPDATING_VIDEO_DATA = "Updating video data"
    const val FINISHING_UP = "Finishing up"
    const val TASK_COMPLETED = "Task completed"
}

class FFMPEGFactory {

    private val outputRootDirPath = "${File.separator}ffmpeg-video-files${File.separator}output"
    private val tempFileDirPath = "${outputRootDirPath}${File.separator}temp-vid"
    private val transcendedDirPath = "${outputRootDirPath}${File.separator}transcended-videos"
    private var currentJob: Job? = null

    fun processTask(task: Task, scope: CoroutineScope): Job {
        currentJob = scope.launch {
            try {
                if (task.retryTimes > 5) return@launch
                if (task.request.record == null) throw Exception("Empty record")
                val videoId = task.request.record.id!!
                val fileKey = task.request.record.videoKey!!
                StatusHelper.setStatus(task, Status.STARTED)
                StatusHelper.setStatus(task, Status.DOWNLOADING_FILE_LOCALLY)
                val filePath = downloadFile(fileKey)
                StatusHelper.setStatus(task, Status.TRANSCODING_VIDEO)
                val resolutionList = transcodeVideoFile(filePath)
                StatusHelper.setStatus(task, Status.UPLOADING_SEGMENTS)
                val m3u8FilesUrls = uploadFilesAndGetM3U8FileUrls(resolutionList, videoId, task.request)
                StatusHelper.setStatus(task, Status.CREATING_PACKAGES)
                val outputFilePath = createM3U8File(resolutionList, videoId, m3u8FilesUrls)
                StatusHelper.setStatus(task, Status.UPLOADING_PACKAGES)
                val outputFileUrl = uploadM3U8File(outputFilePath, "$videoId/${videoId}_master.m3u8")
                StatusHelper.setStatus(task, Status.UPDATING_VIDEO_DATA)
                updateVideoUrl(outputFileUrl, task.request)
                StatusHelper.setStatus(task, Status.FINISHING_UP)
                deleteAllFiles()
                StatusHelper.setStatus(task, Status.TASK_COMPLETED)
            } catch (e: Exception) {
                deleteAllFiles()
                println("\n\n")
                println(e)
                println("\n\n")
                println("Error processing task: ${task.request.record?.id}, due to ${e.message}, retrying...")
                TaskFactory.addTask(task.copy(retryTimes = task.retryTimes + 1))
            }
        }
        return currentJob!!
    }

    fun cancelCurrentTask() {
        deleteAllFiles()
        currentJob?.cancel()
    }

    private fun deleteAllFiles() {
        println("Deleting all files")
        val dir = File(outputRootDirPath)
        if (dir.exists()) dir.deleteRecursively()
        println("Deleted...")
    }


    private suspend fun downloadFile(fileKey: String): String {
        println("Downloading file...")
        File(tempFileDirPath).mkdirs()
        val filePath = "$tempFileDirPath${File.separator}temp-file${
            (fileKey.substring(
                fileKey.lastIndexOf("."),
                fileKey.length
            ))
        }"
        val outputFile = File(filePath)
        Supabase.client.storage.from("temp-lectures").downloadAuthenticatedToAsFlow(fileKey, outputFile)
            .collect { status ->
                when (status) {
                    is DownloadStatus.Success -> {
                        println("\nDownload Success...")
                    }

                    is DownloadStatus.Progress -> {
                        print("\rProgress: ${status.totalBytesReceived * 100 / status.contentLength}%")
                    }

                    else -> {}
                }
            }
        return outputFile.absolutePath
    }

    private suspend fun transcodeVideoFile(inputFilePath: String): List<Resolution> {
        println("Transcoding video...")
        File(transcendedDirPath).mkdirs()
        val resolution = getResolution(inputFilePath)
        val resolutionList = generateResolutionsList(fileResolution = resolution)
        resolutionList.forEach {
            println(it.resolutions)
        }
        println("Process Started...")
        return withContext(Dispatchers.IO) {
            for (it in resolutionList) {
                if (currentJob != null && currentJob!!.isCancelled) break
                println("Started for resolution : ${it.resolutions}")
                val currentOutputDir = "$transcendedDirPath${File.separator}${it.size.second}"
                File(currentOutputDir).mkdirs()
                println("Dir created...")
                val cmd =
                    "ffmpeg -i ${File(inputFilePath).absolutePath} -preset fast -threads 2 -s ${it.size.first}x${it.size.second} -aspect 16:9 -f hls -hls_list_size 1000000 -hls_time 2 $currentOutputDir${File.separator}${it.size.second}_out.m3u8"
                val command = cmd.split(" ")
                val processBuilder = ProcessBuilder(command)
                processBuilder.redirectErrorStream(true)
                val process = processBuilder.start()
                process.inputStream.bufferedReader().forEachLine {
                    println(it)
                }
                process.waitFor()
                println("\nTranscoding completed for resolution : ${it.resolutions}")
            }
            println("Video Transcended!...")
            return@withContext resolutionList
        }
    }

    private suspend fun uploadTsFilesAndGetSignedUrls(
        resolution: String,
        filesDir: String,
        videoId: String
    ): Map<String, String> {
        val dir = File(filesDir)
        val files = dir.listFiles(FileFilter {
            return@FileFilter it.extension != "m3u8"
        })!!.toMutableList().sortedBy { extractNumber(it.nameWithoutExtension) }
        val signedUrls = mutableMapOf<String, String>()
        files.forEach {
            val fileName = it.name
            Supabase.client.storage.from("videos").uploadAsFlow("$videoId/$resolution/$fileName", it)
                .collect { status ->
                    when (status) {
                        is UploadStatus.Progress -> {
                            val progress = status.totalBytesSend * 100 / status.contentLength
                            print("\rUploading file : $fileName -> Progress : $progress%")
                        }

                        is UploadStatus.Success -> Unit
                    }
                }
            val signedUrl =
                Supabase.client.storage.from("videos")
                    .createSignedUrl("$videoId/$resolution/$fileName", Duration.INFINITE)
            signedUrls[fileName] = signedUrl
        }
        return signedUrls
    }

    private suspend fun uploadFilesAndGetM3U8FileUrls(
        resolutionList: List<Resolution>,
        videoId: String,
        request: VideoRequestBody
    ): Map<Int, String> {
        val m3u8FileSignedUrls = mutableMapOf<Int, String>()
        val videoEncodings = mutableMapOf<Int, VideoResolutionModel>()
        resolutionList.forEach {
            val signedUrls =
                uploadTsFilesAndGetSignedUrls(
                    "${it.size.second}",
                    "$transcendedDirPath${File.separator}${it.size.second}${File.separator}",
                    videoId
                )
            val m3u8UpdatedFilePath =
                readAndWriteM3U8File(
                    "$transcendedDirPath${File.separator}${it.size.second}${File.separator}${it.size.second}_out.m3u8",
                    signedUrls
                )
            val resDir = File("$transcendedDirPath${File.separator}${it.size.second}")
            val m3u8SignedUrl =
                uploadM3U8File(m3u8UpdatedFilePath, "$videoId/${it.size.second}/${it.size.second}_output.m3u8")
            val resolutionTotalSize = getFolderSizeRecursive(resDir)
            m3u8FileSignedUrls[it.size.second] = m3u8SignedUrl
            val model = VideoResolutionModel(
                resolution = it.size.second,
                m3u8SignedUrl,
                resolutionTotalSize,
            )
            videoEncodings[it.size.second] = model
        }
        updateVideoEncodings(videoEncodings, request)
        return m3u8FileSignedUrls
    }

    private fun readAndWriteM3U8File(filePath: String, signedUrls: Map<String, String>): String {
        val m3u8File = File(filePath)
        val newM3U8File = File("${m3u8File.parent}${File.separator}${m3u8File.nameWithoutExtension}put.m3u8")
        val lines = m3u8File.readLines()
        newM3U8File.bufferedWriter().use {
            for (line in lines) {
                if (line.startsWith("#")) {
                    it.write(line)
                } else {
                    it.write(signedUrls[line].toString())
                }
                it.newLine()
            }
            it.close()
        }
        return newM3U8File.absolutePath
    }

    private suspend fun uploadM3U8File(filePath: String, storagePath: String): String {
        Supabase.client.storage.from("videos").uploadAsFlow(storagePath, File(filePath)).collect {}
        return Supabase.client.storage.from("videos").createSignedUrl(storagePath, Duration.INFINITE)
    }

    private fun createM3U8File(
        resolutionList: List<Resolution>,
        videoId: String,
        resolutionM3U8FilesUrls: Map<Int, String>
    ): String {
        val finalM3U8File = File(transcendedDirPath, "${videoId}_master.m3u8")
        finalM3U8File.bufferedWriter().use {
            it.write("#EXTM3U")
            it.newLine()
            it.write("#EXT-X-VERSION:3")
            it.newLine()
            it.write("#EXT-X-INDEPENDENT-SEGMENTS")
            for (res in resolutionList) {
                it.newLine()
                it.write("#EXT-X-STREAM-INF:${getBandwidth(res.size.second)},RESOLUTION=${res.size.first}x${res.size.second}")
                it.newLine()
                it.write(resolutionM3U8FilesUrls[res.size.second]!!)
            }
            it.close()
        }
        return finalM3U8File.absolutePath
    }

    private suspend fun updateVideoEncodings(encodings: Map<Int, VideoResolutionModel>, request: VideoRequestBody) {
        Supabase.client.from(request.table!!).update({
            set("video_encodings", encodings)
        }) {
            filter {
                eq("id", request.record!!.id!!)
            }
        }
    }

    private suspend fun updateVideoUrl(videoUrl: String, request: VideoRequestBody) {
        Supabase.client.from(request.table!!).update({
            set("video_url", videoUrl)
            set("visibility", true)
            set("status", "transcended")
        }) {
            filter {
                eq("id", request.record!!.id!!)
            }
        }
    }

    private fun getFolderSizeRecursive(folder: File): Long {
        var totalSize = 0L
        folder.listFiles()?.forEach { file ->
            totalSize += if (file.isFile) file.length() else getFolderSizeRecursive(file)
        }
        return totalSize
    }
}

@Serializable
data class VideoResolutionModel(
    val resolution: Int,
    @SerialName("video_url")
    val videoUrl: String,
    @SerialName("size_in_bytes")
    val sizeInBytes: Long,
)