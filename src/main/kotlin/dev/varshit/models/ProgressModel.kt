package dev.varshit.models

import kotlinx.serialization.Serializable

@Serializable
data class ProgressModel(
    val videoId: String,
    val fileKey: String,
    val message : String,
    val progress : Float,
)
