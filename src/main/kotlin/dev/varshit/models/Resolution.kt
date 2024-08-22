package dev.varshit.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Resolution(
    @SerialName("RESOLUTION") val resolutions: String,
    @SerialName("SIZE") val size: Pair<Int, Int>,
)