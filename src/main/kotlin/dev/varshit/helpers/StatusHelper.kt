package dev.varshit.helpers

import dev.varshit.factory.Status
import dev.varshit.models.Task
import dev.varshit.plugins.Supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object StatusHelper {

    @Serializable
    data class Msg(
        val id: String,
        val status: String,
        @SerialName("temp_video_key")
        val videoKey: String,
        val working: Boolean = true,
        val visibility: Boolean = false,
    )

    suspend fun setStatus(task: Task, status: String) {
        if (status == Status.TASK_COMPLETED) {
            Supabase.client.from(task.request.table!!)
                .upsert(Msg(task.request.record!!.id!!, status, task.request.record.videoKey!!, working = false, visibility = true))
        }
        Supabase.client.from(task.request.table!!).upsert(Msg(task.request.record!!.id!!, status, task.request.record.videoKey!!))
        Supabase.client.from("video-transcoder").upsert(Msg(task.request.record.id!!, status, task.request.record.videoKey))
    }

}