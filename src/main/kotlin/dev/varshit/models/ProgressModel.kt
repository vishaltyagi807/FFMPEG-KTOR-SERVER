package dev.varshit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProgressModel(
    @SerialName("video_id")
    val videoId: String,
    @SerialName("file_key")
    val fileKey: String,
    @SerialName("message")
    val message : String,
    @SerialName("progress")
    val progress : Float,
)
