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
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.serialization.json.*

val KtorBlockNsfwEndpoints = createApplicationPlugin("HanaBlockNsfwEndpoints") {
    val ROUTE_REGEX = "(\\/v\\d)?\\/yiff?(\\/(\\w+)*)?".toRegex()
    val jwt: JwtManager by inject()

    onCall { call ->
        if (!call.request.uri.matches(ROUTE_REGEX)) {
            return@onCall
        }

        val matcher = ROUTE_REGEX.toPattern().matcher(call.request.uri)
        if (!matcher.matches()) {
            return@onCall
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

        // Do we have any authorization keys?
        val auth = call.request.header("Authorization")
        if (auth == null) {
            call.respond(
                HttpStatusCode.Forbidden,
                buildJsonObject {
                    if (version == 3) {
                        put("success", false)
                        put(
                            "errors",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("code", "FORBIDDEN")
                                        put("message", "You cannot access blocked endpoints without proper authorization!")
                                        put("detail", "You are missing the `Authorization` header in your request.")
                                    }
                                )
                            }
                        )
                    } else {
                        put("message", "You are missing the `Authorization` header in your request, which means you cannot access blocked endpoints.")
                    }
                }
            )

            return@onCall
        }

        val params = auth.split(" ")
        if (params.isEmpty() || params.size > 2) {
            call.respond(
                HttpStatusCode.Forbidden,
                buildJsonObject {
                    if (version == 3) {
                        put("success", false)
                        put(
                            "errors",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("code", "UNKNOWN_AUTH_STRUCTURE")
                                        put("message", "You are missing the proper authorization structure.")
                                        put("detail", "Make sure your Authorization header is: \"Bearer <token>\"")
                                    }
                                )
                            }
                        )
                    } else {
                        put("message", "Authorization structure was not correct, please format it as so: \"Bearer <token>\"")
                    }
                }
            )

            return@onCall
        }

        if (params[0] != "Bearer") {
            call.respond(
                HttpStatusCode.Forbidden,
                buildJsonObject {
                    if (version == 3) {
                        put("success", false)
                        put(
                            "errors",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("code", "UNKNOWN_PREFIX")
                                        put("message", "The prefix of the Authorization value was not \"Bearer\"")
                                        put("detail", "Make sure your Authorization header is: \"Bearer <token>\"")
                                    }
                                )
                            }
                        )
                    } else {
                        put("message", "Authorization structure was not correct, please format it as so: \"Bearer <token>\"")
                    }
                }
            )

            return@onCall
        }

        if (!jwt.isValid(params[1])) {
            call.respond(
                HttpStatusCode.Forbidden,
                buildJsonObject {
                    if (version == 3) {
                        put("success", false)
                        put(
                            "errors",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("code", "INVALID_TOKEN")
                                        put("message", "Token provided was not a valid token")
                                    }
                                )
                            }
                        )
                    } else {
                        put("message", "Invalid token was provided, please register one in the Noelware Discord server: https://discord.gg/ATmjFH9kMH")
                    }
                }
            )

            return@onCall
        }

        // Check if it's in the database
        val apiKey = asyncTransaction {
            ApiKeyEntity.find {
                ApiKeysTable.token eq params[1]
            }.firstOrNull()
        }

        if (apiKey == null) {
            call.respond(
                HttpStatusCode.Forbidden,
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

            return@onCall
        }

        val perms = apiKey.permissions?.split("|")?.contains("nsfw") ?: false
        if (!perms) {
            call.respond(
                HttpStatusCode.Forbidden,
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

            return@onCall
        }
    }
}
