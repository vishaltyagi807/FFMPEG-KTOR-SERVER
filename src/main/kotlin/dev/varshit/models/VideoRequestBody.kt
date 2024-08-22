package dev.varshit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoRequestBody(
    val table: String?,
    val schema: String?,
    val record: VideoRequest?,
)

@Serializable
data class VideoRequest(
    val id: String?,
    @SerialName("temp_video_key")
    val videoKey: String?,
)
