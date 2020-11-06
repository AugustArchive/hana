package dev.floofy.api.data

import kotlinx.serialization.Serializable

@Serializable
data class Application(
    val version: String,
    val commit: String
)
