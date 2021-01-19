package dev.floofy.api.data

import kotlinx.serialization.Serializable

@Serializable
data class RedisConfig(
    val password: String? = null,
    val host: String = "localhost",
    val port: Int = 6379,
    val db: Int = 2
)
