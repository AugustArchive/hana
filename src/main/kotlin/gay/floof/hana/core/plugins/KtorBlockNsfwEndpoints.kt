/*
 * 🥀 hana: API to proxy different APIs like GitHub Sponsors, source code for api.floofy.dev
 * Copyright (c) 2020-2022 Noel <cutie@floofy.dev>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package gay.floof.hana.core.plugins

import gay.floof.hana.core.database.asyncTransaction
import gay.floof.hana.core.database.tables.ApiKeyEntity
import gay.floof.hana.core.database.tables.ApiKeysTable
import gay.floof.hana.core.extensions.inject
import gay.floof.hana.core.managers.JwtManager
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.*
import kotlinx.serialization.json.*

class KtorBlockNsfwEndpoints {
    companion object: ApplicationFeature<ApplicationCallPipeline, Unit, KtorBlockNsfwEndpoints> {
        private val ROUTE_REGEX = "\\/api\\/?(\\/v\\d)?\\/yiff?(\\/(\\w+)*)?".toRegex()

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
                        json.encodeToString(
                            JsonObject.serializer(),
                            buildJsonObject {
                                if (version == 3) {
                                    put("success", false)
                                    put(
                                        "errors",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("code", "BLOCKED_ENDPOINT")
                                                    put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                                    put("where", "Authorization header is not present")
                                                }
                                            )
                                        }
                                    )
                                } else {
                                    put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                }
                            }
                        )
                    }

                    finish()
                    return@intercept
                }

                // Check if the API key is valid
                val jwt: JwtManager by inject()
                val params = authorization.split(" ")

                if (params[0] != "Bearer") {
                    call.respondText(contentType = ContentType.parse("application/json"), HttpStatusCode.Forbidden) {
                        json.encodeToString(
                            JsonObject.serializer(),
                            buildJsonObject {
                                if (version == 3) {
                                    put("success", false)
                                    put(
                                        "errors",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("code", "BLOCKED_ENDPOINT")
                                                    put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                                    put("where", "Authorization header prefix was not 'Bearer'")
                                                }
                                            )
                                        }
                                    )
                                } else {
                                    put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                }
                            }
                        )
                    }

                    finish()
                    return@intercept
                }

                if (!jwt.isValid(params[1])) {
                    call.respondText(contentType = ContentType.parse("application/json"), HttpStatusCode.Forbidden) {
                        json.encodeToString(
                            JsonObject.serializer(),
                            buildJsonObject {
                                if (version == 3) {
                                    put("success", false)
                                    put(
                                        "errors",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("code", "BLOCKED_ENDPOINT")
                                                    put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                                    put("where", "JWT header was not valid")
                                                }
                                            )
                                        }
                                    )
                                } else {
                                    put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                }
                            }
                        )
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
                        json.encodeToString(
                            JsonObject.serializer(),
                            buildJsonObject {
                                if (version == 3) {
                                    put("success", false)
                                    put(
                                        "errors",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("code", "BLOCKED_ENDPOINT")
                                                    put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                                    put("where", "API key was not found!")
                                                }
                                            )
                                        }
                                    )
                                } else {
                                    put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                }
                            }
                        )
                    }

                    finish()
                    return@intercept
                }

                // Check if the API key has permissions
                val perms = apiKey.permissions?.split("|")
                if (perms?.contains("nsfw") == false) {
                    call.respondText(contentType = ContentType.parse("application/json"), HttpStatusCode.Forbidden) {
                        json.encodeToString(
                            JsonObject.serializer(),
                            buildJsonObject {
                                if (version == 3) {
                                    put("success", false)
                                    put(
                                        "errors",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("code", "BLOCKED_ENDPOINT")
                                                    put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                                    put("where", "API key does not have permissions to use NSFW endpoints. :<")
                                                }
                                            )
                                        }
                                    )
                                } else {
                                    put("message", "You are not allowed to preview NSFW endpoints without an API key.")
                                }
                            }
                        )
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
