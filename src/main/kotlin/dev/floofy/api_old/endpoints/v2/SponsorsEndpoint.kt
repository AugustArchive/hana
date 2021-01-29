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

package dev.floofy.api_old.endpoints.v2

import dev.floofy.api_old.core.Endpoint
import dev.floofy.api_old.data.Config
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SponsorsEndpoint: Endpoint(HttpMethod.GET, "/sponsors") {
    override fun run(ctx: RoutingContext) {
        val res = ctx.response()
        return res.setStatusCode(406).end(JsonObject().apply {
            put("message", "Missing `login` path param.")
        })
    }
}

class ListSponsorsEndpoint(
    private val config: Config,
    private val http: WebClient
): Endpoint(HttpMethod.GET, "/sponsors/:login") {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun run(ctx: RoutingContext) {
        val res = ctx.response()
        val req = ctx.request()
        val params = req.params()

        if (config.githubAccessToken == null) return res.setStatusCode(400).end(JsonObject().apply {
            put("statusCode", 400)
            put("message", "Missing `github_access_token` key, unable to collect sponsors")
        })

        val login = params.get("login")

        val first = try {
            Integer.parseInt(params.get("first") ?: "5")
        } catch (ex: Exception) {
            null
        } ?: return res.setStatusCode(406).end(JsonObject().apply {
            put("message", "Argument `first` was not a proper number.")
        })

        val pricing = when (params.get("pricing")) {
            "cents" -> "cents" // put it as cents
            "dollars" -> "dollars" // put it as dollars
            null -> "dollars" // nothing was provide, default to dollars

            else -> null
        } ?: return res.setStatusCode(406).end(JsonObject().apply {
            put("message", "Argument `pricing` was invalid, must receive `dollars` or `cents`")
        })

        val showPrivate = java.lang.Boolean.parseBoolean(params.get("show_private") ?: "false")
        val monthlyIn = when (pricing) {
            "dollars" -> "monthlyPriceInDollars"
            "cents" -> "monthlyPriceInCents"
            else -> "monthlyPriceInDollars" // we are required to add this unless it fails?
        }

        val query = """
            query {
                # TODO: Add organization details?
                user(login: "$login") {
                    # Fetches the sponsors as the maintainer
                    sponsorshipsAsMaintainer(first: $first) {
                        # How many sponsors the user has
                        totalCount

                        # The nodes to get the data we want
                        nodes {
                            # The level the sponsor is as (PRIVATE or PUBLIC)
                            privacyLevel

                            # When they started sponsoring
                            createdAt

                            # The tier itself
                            tier {
                                $monthlyIn
                                name
                            }

                            # The actual entity
                            sponsorEntity {
                                # If it's a User account
                                ... on User {
                                    # Display how many followers they have
                                    followers(first: $first) {
                                        totalCount
                                    }

                                    # Display how many people they are following
                                    following(first: $first) {
                                        totalCount
                                    }

                                    # The avatar URL, fixed size to 1024
                                    avatarUrl(size: 1024)

                                    # The website URL they are displaying
                                    websiteUrl

                                    # The company they work at
                                    company

                                    # The user's username
                                    login

                                    # Their name listed
                                    name

                                    # Their bio
                                    bio

                                    # Their profile URL
                                    url
                                }
                            }
                        }
                    }

                    sponsorshipsAsSponsor(first: $first) {
                        totalCount
                        nodes {
                            privacyLevel
                            createdAt
                            tier {
                                $monthlyIn
                                name
                            }

                            sponsorable {
                                ... on User {
                                    followers(first: $first) {
                                        totalCount
                                    }

                                    following(first: $first) {
                                        totalCount
                                    }

                                    avatarUrl(size: 1024)
                                    websiteUrl
                                    company
                                    login
                                    name
                                    bio
                                    url
                                }

                                ... on Organization {
                                    avatarUrl(size: 1024)
                                    twitterUsername
                                    description
                                    websiteUrl
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

        return http.requestAbs(HttpMethod.POST, "https://api.github.com/graphql")
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

                val result = it.result()
                val obj = result.bodyAsJsonObject()
                val isError = obj.getString("message", null) != null
                val isGraphQLError = obj.getJsonArray("errors", null) != null

                if (isError) return@sendJsonObject res.setStatusCode(406).end(JsonObject().apply {
                    put("message", obj.getString("message"))
                })

                if (isGraphQLError) return@sendJsonObject res.setStatusCode(406).end(JsonObject().apply {
                    put("message", obj.getJsonArray("errors"))
                    put("notice", "If this keeps being occuring, report it to August#5820 or https://t.me/auguwu (if you use Telegram)")
                })

                val sponsorData = obj.getJsonObject("data", null)
                    ?: return@sendJsonObject res.setStatusCode(500).end(JsonObject().apply {
                        put("message", "Didn't receive any data from GitHub, is it down?")
                    })

                val user = sponsorData.getJsonObject("user", null)
                    ?: return@sendJsonObject res.setStatusCode(500).end(JsonObject().apply {
                        put("message", "Didn't receive any data from GitHub, is it down?")
                    })

                val sponsorships = user.getJsonObject("sponsorshipsAsMaintainer", null)
                    ?: return@sendJsonObject res.setStatusCode(500).end(JsonObject().apply {
                        put("message", "Didn't receive any data from GitHub, is it down?")
                    })

                val total = sponsorships.getInteger("totalCount", 0)
                val all = sponsorships.getJsonArray("nodes", null)
                    ?: return@sendJsonObject res.setStatusCode(500).end(JsonObject().apply {
                        put("message", "Didn't receive any data from GitHub, is it down?")
                    })

                val sponsors = user.getJsonObject("sponsorshipsAsSponsor", null)
                    ?: return@sendJsonObject res.setStatusCode(500).end(JsonObject().apply {
                        put("message", "Didn't receive any data from GitHub, is it down?")
                    })

                val totalSponsors = sponsors.getInteger("totalCount", 0)
                val userSponsors = sponsors.getJsonArray("nodes", null)
                    ?: return@sendJsonObject res.setStatusCode(500).end(JsonObject().apply {
                        put("message", "Didn't receive any data from GitHub, is it down?")
                    })

                val allSponsors = all
                    .list
                    .map { sponsor ->
                        if (sponsor is LinkedHashMap<*, *>) {
                            val d = sponsor
                            return@map getSponsor(d, showPrivate)
                        } else {
                            return@map null
                        }
                    }.filterNotNull()

                val allUserSponsors = userSponsors
                    .list
                    .map { sponsor ->
                        if (sponsor is LinkedHashMap<*, *>) {
                            val d = sponsor
                            return@map getSponsor(d, showPrivate, true)
                        } else {
                            return@map null
                        }
                    }.filterNotNull()

                return@sendJsonObject res.setStatusCode(200).end(JsonObject().apply {
                    put("sponsors", JsonObject().apply {
                        put("total_count", total)
                        put("data", allSponsors)
                    })

                    put("user_sponsors", JsonObject().apply {
                        put("total_count", totalSponsors)
                        put("data", allUserSponsors)
                    })
                })
            }
    }

    private fun getSponsor(data: LinkedHashMap<*, *>, showPrivate: Boolean, isUser: Boolean = false): JsonObject? {
        val sponsor = data as LinkedHashMap<String, Any> // idfk what to fix
        val privacyLevel = sponsor.getOrDefault("privacyLevel", "PUBLIC")

        if (!showPrivate && privacyLevel == "PRIVATE") return null

        val key = when (isUser) {
            false -> "sponsorEntity"
            true -> "sponsorable"
        }

        return JsonObject().apply {
            put("created_at", sponsor["createdAt"])
            put("tier", sponsor["tier"])
            put("user", JsonObject().apply {
                val user = sponsor[key] as LinkedHashMap<String, Any>
                val followers = user["followers"] as LinkedHashMap<String, Any>
                val following = user["following"] as LinkedHashMap<String, Any>

                put("website_url", user["websiteUrl"])
                put("avatar_url", user["avatarUrl"])
                put("following", following["totalCount"])
                put("followers", followers["totalCount"])
                put("company", user["company"])
                put("login", user["login"])
                put("name", user["name"])
                put("bio", user["bio"])
                put("url", user["url"])
            })
        }
    }
}
