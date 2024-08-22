package dev.varshit.models

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val retryTimes: Int,
    val request: VideoRequestBody,
)
