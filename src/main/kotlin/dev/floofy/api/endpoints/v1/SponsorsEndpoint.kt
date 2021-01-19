/**
 * Copyright (c) 2020-2021 August
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
package dev.floofy.api.endpoints.v1

import dev.floofy.api.core.Endpoint
import dev.floofy.api.data.Config
import dev.floofy.api.end
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SponsorsEndpoint(
    private val config: Config,
    private val http: WebClient
): Endpoint(HttpMethod.GET, "/sponsors", 1) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun run(ctx: RoutingContext) {
        val res = ctx.response()
        val req = ctx.request()

        if (config.githubAccessToken == null) return res.setStatusCode(400).end(JsonObject().apply {
            put("statusCode", 400)
            put("message", "Missing `github_access_token` key, unable to collect sponsors")
        })

        val params = req.params()
        val login = params.get("login") ?: return res.setStatusCode(406).end(JsonObject().apply {
            put("statusCode", 406)
            put("message", "Missing `?login` query")
        })

        val first = (try {
            Integer.parseInt(params.get("first") ?: "5")
        } catch (ex: Exception) {
            null
        }) ?: return res.setStatusCode(406).end(JsonObject().apply {
            put("statusCode", 406)
            put("message", "Argument `first` was not a proper number.")
        })

        val query = """
            query {
                user(login: "$login") {
                    sponsorshipsAsMaintainer(first: $first) {
                        nodes {
                            privacyLevel
                            createdAt
                            tier {
                                monthlyPriceInCents
                                name
                            }

                            sponsorEntity {
                                ... on User {
                                    avatarUrl(size: 1024)
                                    login
                                    name
                                    bio
                                    url
                                }

                                ... on Organization {
                                    avatarUrl(size: 1024)
                                    description
                                    login
                                    url
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        val body = JsonObject().apply {
            put("query", query)
        }

        return http
                .requestAbs(HttpMethod.POST, "https://api.github.com/graphql")
                .putHeader("Authorization", "Bearer ${config.githubAccessToken}")
                .sendJsonObject(body) {
                    if (it.failed()) {
                        val b = it.cause()
                        logger.error("Unable to make a request to GitHub:")
                        logger.error(b.toString())

                        return@sendJsonObject res.setStatusCode(400).end(JsonObject().apply {
                            put("statusCode", 400)
                            put("message", "Unknown error, if you are running this -- check console.")
                        })
                    }

                    val r = it.result()
                    val obj = r.bodyAsJsonObject()
                    val isError = obj.getString("message", null) != null

                    if (isError) return@sendJsonObject res.setStatusCode(500).end(JsonObject().apply {
                        put("statusCode", 400)
                        put("message", obj.getString("message"))
                    })

                    val graphqlError = obj.getJsonArray("errors", null) != null
                    if (graphqlError) return@sendJsonObject res.setStatusCode(406).end(JsonObject().apply {
                        put("statusCode", 406)
                        put("errors", obj.getJsonArray("errors"))
                    })

                    val data = obj.getJsonObject("data", null)
                            ?: return@sendJsonObject res.setStatusCode(406).end(JsonObject().apply {
                                put("statusCode", 406)
                                put("message", "Didn't receive any data from GitHub, is it down?")
                            })

                    val user = data.getJsonObject("user", null)
                            ?: return@sendJsonObject res.setStatusCode(406).end(JsonObject().apply {
                                put("statusCode", 406)
                                put("message", "Didn't receive any data from GitHub, is it down?")
                            })

                    val sponsorships = user.getJsonObject("sponsorshipsAsMaintainer", null)
                            ?: return@sendJsonObject res.setStatusCode(406).end(JsonObject().apply {
                                put("statusCode", 406)
                                put("message", "Didn't receive any data from GitHub, is it down?")
                            })

                    val all = sponsorships.getJsonArray("nodes", null)
                            ?: return@sendJsonObject res.setStatusCode(406).end(JsonObject().apply {
                                put("statusCode", 406)
                                put("message", "Didn't receive any data from GitHub, is it down?")
                            })

                    val formatted = all
                            .list
                            .map { sponsor ->
                                if (sponsor != null && sponsor is LinkedHashMap<*, *>) {
                                    return@map getSponsor(sponsor)
                                } else {
                                    return@map null
                                }
                            }.filterNotNull()

                    return@sendJsonObject res.setStatusCode(200).end(JsonObject().apply {
                        put("statusCode", 200)
                        put("data", formatted)
                    })
                }
    }

    private fun getSponsor(data: LinkedHashMap<*, *>): JsonObject? {
        val sponsor = data as LinkedHashMap<String, Any> // idk what else to do?
        if (sponsor.getOrDefault("privacyLevel", "PUBLIC") == "PRIVATE") return null

        return JsonObject().apply {
            put("createdAt", data["createdAt"])
            put("sponsor", JsonObject().apply {
                val d = data["sponsorEntity"] as LinkedHashMap<String, Any> // literally i dont care lol

                put("profile", d["url"])
                put("avatar", d["avatarUrl"])
                put("login", d["login"])
                put("name", d["name"])
                put("bio", d["bio"])
            })
            put("tier", data["tier"])
        }
    }
}
