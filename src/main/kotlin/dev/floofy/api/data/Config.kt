package dev.floofy.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    @SerialName("github_access_token")
    val githubAccessToken: String,

    @SerialName("webhook_url")
    val webhookUrl: String,

    @SerialName("port")
    val port: Int
)
