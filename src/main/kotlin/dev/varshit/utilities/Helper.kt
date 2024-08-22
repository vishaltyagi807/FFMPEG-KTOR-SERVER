package dev.varshit.utilities

import dev.varshit.models.Resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object Helper {


    fun extractNumber(fileName: String): Int {
        val regex = "_out(\\d+)".toRegex()
        val matchResult = regex.find(fileName)
        return matchResult?.groupValues?.get(1)?.toInt() ?: Int.MAX_VALUE
    }

    fun getBandwidth(fileResolution: Int): String {
        if (fileResolution <= 144) {
            return "BANDWIDTH=12500,AVERAGE-BANDWIDTH=8000"
        } else if (fileResolution <= 270) {
            return "BANDWIDTH=37500,AVERAGE-BANDWIDTH=18750"
        } else if (fileResolution <= 360) {
            return "BANDWIDTH=100000,AVERAGE-BANDWIDTH=37500"
        } else if (fileResolution <= 480) {
            return "BANDWIDTH=250000,AVERAGE-BANDWIDTH=62500"
        } else if (fileResolution <= 720) {
            return "BANDWIDTH=625000,AVERAGE-BANDWIDTH=187500"
        } else if (fileResolution <= 1080) {
            return "BANDWIDTH=1000000,AVERAGE-BANDWIDTH=375000"
        } else if (fileResolution <= 1440) {
            return "BANDWIDTH=2000000,AVERAGE-BANDWIDTH=750000"
        } else if (fileResolution <= 2160) {
            return "BANDWIDTH=5000000,AVERAGE-BANDWIDTH=1875000"
        } else {
            return "BANDWIDTH=5000000,AVERAGE-BANDWIDTH=1875000"
        }
    }

    suspend fun getResolution(filePath: String): Pair<Int, Int> {
        return withContext(Dispatchers.IO) {
            val ffprobeCommand = listOf(
                "ffprobe",
                "-v",
                "error",
                "-select_streams",
                "v:0",
                "-show_entries",
                "stream=width,height",
                "-of",
                "default=noprint_wrappers=1:nokey=1",
                filePath
            )
            val processBuilder = ProcessBuilder(ffprobeCommand)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val width = reader.readLine().toInt()
            val height = reader.readLine()!!.toInt()
            process.waitFor()
            Pair(width, height)
        }
    }


    fun generateResolutionsList(fileResolution: Pair<Int, Int>): List<Resolution> {
        if (fileResolution.second <= 144) {
            return listOf(Resolution("144p", fileResolution))
        } else if (fileResolution.second <= 360) {
            return listOf(Resolution("144p", Pair(256, 144)), Resolution("360p", fileResolution))
        } else if (fileResolution.second <= 480) {
            return listOf(
                Resolution("144p", Pair(256, 144)),
                Resolution("360p", Pair(640, 360)),
                Resolution("480p", fileResolution)
            )
        } else if (fileResolution.second <= 720) {
            return listOf(
                Resolution("144p", Pair(256, 144)),
                Resolution("360p", Pair(640, 360)),
                Resolution("480p", Pair(920, 480)),
                Resolution("720p", fileResolution)
            )
        } else if (fileResolution.second <= 1080) {
            return listOf(
                Resolution("144p", Pair(256, 144)),
                Resolution("360p", Pair(640, 360)),
                Resolution("480p", Pair(720, 480)),
                Resolution("720p", Pair(1280, 720)),
                Resolution("1080p", fileResolution)
            )
        } else if (fileResolution.second <= 1440) {
            return listOf(
                Resolution("144p", Pair(256, 144)),
                Resolution("360p", Pair(640, 360)),
                Resolution("480p", Pair(720, 480)),
                Resolution("720p", Pair(1280, 720)),
                Resolution("1080p", Pair(1920, 1080)),
                Resolution("1440p", fileResolution),
            )
        } else if (fileResolution.second <= 2160) {
            return listOf(
                Resolution("144p", Pair(256, 144)),
                Resolution("360p", Pair(640, 360)),
                Resolution("480p", Pair(720, 480)),
                Resolution("720p", Pair(1280, 720)),
                Resolution("1080p", Pair(1920, 1080)),
                Resolution("1440p", Pair(2560, 1440)),
                Resolution("2160p", fileResolution),
            )
        } else {
            return listOf(
                Resolution("144p", Pair(256, 144)),
                Resolution("360p", Pair(640, 360)),
                Resolution("480p", Pair(720, 480)),
                Resolution("720p", Pair(1280, 720)),
                Resolution("1080p", Pair(1920, 1080)),
                Resolution("1440p", Pair(2560, 1440)),
                Resolution("2160p", Pair(3840, 2160)),
                Resolution("4320p", fileResolution),
            )
        }
    }
}