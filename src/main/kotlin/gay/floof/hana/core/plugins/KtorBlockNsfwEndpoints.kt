package gay.floof.hana.core.plugins

import gay.floof.hana.core.database.asyncTransaction
import gay.floof.hana.core.database.tables.ApiKeyEntity
import gay.floof.hana.core.database.tables.ApiKeysTable
import gay.floof.hana.core.extensions.inject
import gay.floof.hana.core.managers.JwtManager
import gay.floof.hana.utils.toJsonArray
import gay.floof.hana.utils.toJsonPrimitive
import gay.floof.hana.utils.writeJson
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.*
import kotlinx.serialization.json.*

class KtorBlockNsfwEndpoints {
    companion object: ApplicationFeature<ApplicationCallPipeline, Unit, KtorBlockNsfwEndpoints> {
        private val ROUTE_REGEX = "\\/api\\/?(\\/v\\d)?\\/(manipulation|yiff)?(\\/(\\w+)*)?".toRegex()

        override val key: AttributeKey<KtorBlockNsfwEndpoints> = AttributeKey("KtorBlockNsfwEndpoints")
        override fun install(pipeline: ApplicationCallPipeline, configure: Unit.() -> Unit): KtorBlockNsfwEndpoints {
            pipeline.intercept(ApplicationCallPipeline.Call) {
                val json: Json by inject()

                if (!call.request.uri.matches(ROUTE_REGEX)) {
                    proceed()
                    return@intercept
                }

                val matcher = ROUTE_REGEX.toPattern().matcher(call.request.uri)
                if (!matcher.matches()) {
                    proceed()
                    return@intercept
                }

                val groupCount = matcher.groupCount()

                // get all matches
                val matches = mutableListOf<String>()
                for (index in 0..groupCount) {
                    val value = matcher.group(index) ?: continue
                    matches.add(value)
                }

                val versionMatch = matches.find { it.matches("\\/v\\d".toRegex()) }
                val version =
                    if (versionMatch != null) Integer.parseInt(versionMatch.substring(2)) else 3

                // Check for any API keys
                val authorization = call.request.header("Authorization")
                if (authorization == null) {
                    call.respondText(contentType = ContentType.parse("application/json"), HttpStatusCode.Forbidden) {
                        json.encodeToString(JsonObject.serializer(), buildJsonObject {
                            if (version == 3) {
                                put("success", false)
                                put("errors", buildJsonArray {
                                    add(buildJsonObject {
                                        put("code", "BLOCKED_ENDPOINT")
                                        put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                        put("where", "Authorization header is not present")
                                    })
                                })
                            } else {
                                put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                            }
                        })
                    }

                    finish()
                    return@intercept
                }

                // Check if the API key is valid
                val jwt: JwtManager by inject()
                val params = authorization.split(" ")

                if (params[0] != "Bearer") {
                    call.respondText(contentType = ContentType.parse("application/json"), HttpStatusCode.Forbidden) {
                        json.encodeToString(JsonObject.serializer(), buildJsonObject {
                            if (version == 3) {
                                put("success", false)
                                put("errors", buildJsonArray {
                                    add(buildJsonObject {
                                        put("code", "BLOCKED_ENDPOINT")
                                        put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                        put("where", "Authorization header prefix was not 'Bearer'")
                                    })
                                })
                            } else {
                                put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                            }
                        })
                    }

                    finish()
                    return@intercept
                }

                if (!jwt.isValid(params[1])) {
                    call.respondText(contentType = ContentType.parse("application/json"), HttpStatusCode.Forbidden) {
                        json.encodeToString(JsonObject.serializer(), buildJsonObject {
                            if (version == 3) {
                                put("success", false)
                                put("errors", buildJsonArray {
                                    add(buildJsonObject {
                                        put("code", "BLOCKED_ENDPOINT")
                                        put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                        put("where", "JWT header was not valid")
                                    })
                                })
                            } else {
                                put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                            }
                        })
                    }

                    finish()
                    return@intercept
                }

                // Check if it's in the database
                val apiKey = asyncTransaction {
                    ApiKeyEntity.find {
                        ApiKeysTable.token eq params[1]
                    }.firstOrNull()
                }

                if (apiKey == null) {
                    call.respondText(contentType = ContentType.parse("application/json"), HttpStatusCode.Forbidden) {
                        json.encodeToString(JsonObject.serializer(), buildJsonObject {
                            if (version == 3) {
                                put("success", false)
                                put("errors", buildJsonArray {
                                    add(buildJsonObject {
                                        put("code", "BLOCKED_ENDPOINT")
                                        put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                        put("where", "API key was not found!")
                                    })
                                })
                            } else {
                                put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                            }
                        })
                    }

                    finish()
                    return@intercept
                }

                // Check if the API key has permissions
                val perms = apiKey.permissions?.split("|")
                if (perms?.contains("nsfw") == false) {
                    call.respondText(contentType = ContentType.parse("application/json"), HttpStatusCode.Forbidden) {
                        json.encodeToString(JsonObject.serializer(), buildJsonObject {
                            if (version == 3) {
                                put("success", false)
                                put("errors", buildJsonArray {
                                    add(buildJsonObject {
                                        put("code", "BLOCKED_ENDPOINT")
                                        put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                        put("where", "API key does not have permissions to use NSFW endpoints. :<")
                                    })
                                })
                            } else {
                                put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                            }
                        })
                    }

                    finish()
                    return@intercept
                }

                proceed()
            }

            return KtorBlockNsfwEndpoints()
        }
    }
}
