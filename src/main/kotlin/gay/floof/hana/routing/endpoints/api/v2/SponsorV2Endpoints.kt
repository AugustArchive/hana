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

package gay.floof.hana.routing.endpoints.api.v2

import gay.floof.hana.data.HanaConfig
import gay.floof.hana.routing.AbstractEndpoint
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.response.*
import kotlinx.serialization.json.*

private fun graphQlQuery(login: String, pricing: String = "Dollars"): String = """query {
    user(login: "$login") {
        # funni number yes
        sponsorshipsAsMaintainer(last: 69) {
            totalCount
            nodes {
                createdAt
                privacyLevel
                sponsorEntity {
                    ... on Organization {
                        avatarUrl(size: 1024)

                        websiteUrl
                        description
                        isVerified
                        login
                        name
                    }

                    ... on User {
                        followers(last: 69) {
                            totalCount
                        }

                        following(last: 69) {
                            totalCount
                        }

                        status {
                            emojiHTML
                            message
                            expiresAt
                        }

                        websiteUrl
                        twitterUsername
                        hasSponsorsListing
                        avatarUrl(size: 1024)
                        company
                        login
                        name
                        bio
                    }
                }

                tier {
                    monthlyPriceInDollars
                    isCustomAmount
                    createdAt
                    name
                }

                tierSelectedAt
            }
        }

        sponsorshipsAsSponsor(last: 69) {
            totalCount
            nodes {
                createdAt
                privacyLevel
                sponsorable {
                    ... on Organization {
                        avatarUrl(size: 1024)

                        websiteUrl
                        description
                        isVerified
                        login
                        name
                    }

                    ... on User {
                        followers(last: 69) {
                            totalCount
                        }

                        following(last: 69) {
                            totalCount
                        }

                        status {
                            emojiHTML
                            message
                            expiresAt
                        }

                        websiteUrl
                        twitterUsername
                        hasSponsorsListing
                        avatarUrl(size: 1024)
                        company
                        login
                        name
                        bio
                    }
                }

                tier {
                    monthlyPriceIn$pricing
                    isCustomAmount
                    createdAt
                    name
                }

                tierSelectedAt
            }
        }
    }
}
""".trimIndent()

class SponsorV2Endpoint: AbstractEndpoint("/v2/sponsors", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        call.respond(
            buildJsonObject {
                put("message", "Missing :login params.")
            }
        )
    }
}

class FetchSponsorV2Endpoint(private val config: HanaConfig, private val httpClient: HttpClient): AbstractEndpoint("/v2/sponsors/{login}", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        val login = call.parameters["login"]!!

        if (config.githubSecret == null) {
            call.respond(
                HttpStatusCode.InternalServerError,
                buildJsonObject {
                    put("message", "Missing GitHub secret to use this endpoint.")
                }
            )

            return
        }

        val pricing = when (call.request.queryParameters["pricing"]) {
            "cents", "cent", "c" -> "Cents"
            "dollars", "dollar", "d" -> "Dollars"
            else -> "Cents"
        }

        val res: HttpResponse = httpClient.post("https://api.github.com/graphql") {
            header("Content-Type", "application/json")
            header("Authorization", "Bearer ${config.githubSecret}")

            body = buildJsonObject {
                put("query", graphQlQuery(login, pricing))
            }
        }

        val data = res.receive<JsonObject>()
        if (data["errors"]?.jsonArray != null) {
            call.respond(HttpStatusCode.InternalServerError, data["errors"]!!.jsonArray)
            return
        }

        val sponsoring = data["data"]!!.jsonObject["user"]!!.jsonObject["sponsorshipsAsSponsor"]!!.jsonObject
        val sponsors = data["data"]!!.jsonObject["user"]!!.jsonObject["sponsorshipsAsMaintainer"]!!.jsonObject

        println(sponsoring)

        call.respond(
            HttpStatusCode.OK,
            buildJsonObject {
                put(
                    "sponsorsing",
                    buildJsonObject {
                        put("total_count", sponsoring["totalCount"]!!)
                        put(
                            "data",
                            buildJsonArray {
                                val sponsorData = sponsoring["nodes"]!!.jsonArray.toList().map { it.jsonObject }

                                for (obj in sponsorData) {
                                    val tier = obj["tier"]!!.jsonObject
                                    add(
                                        buildJsonObject {
                                            put("joined_at", obj["createdAt"]!!)
                                            put("tier_selected_at", obj["tierSelectedAt"]!!)
                                            put(
                                                "tier",
                                                buildJsonObject {
                                                    put("custom_amount", tier["isCustomAmount"]!!)
                                                    put("created_at", tier["createdAt"]!!)
                                                    put("name", tier["name"]!!)
                                                    put(
                                                        "price",
                                                        if (pricing == "Dollars") {
                                                            tier["monthlyPriceInDollars"]!!
                                                        } else {
                                                            tier["monthlyPriceInCents"]!!
                                                        }
                                                    )
                                                }
                                            )

                                            val sponsorEntity = obj["sponsorable"]!!.jsonObject

                                            put("followers", sponsorEntity["following"]!!)
                                            put("followers", sponsorEntity["followers"]!!)

                                            val status = try {
                                                sponsorEntity["status"]?.jsonObject
                                            } catch (e: IllegalArgumentException) {
                                                null
                                            }

                                            if (status != null) {
                                                put(
                                                    "status",
                                                    buildJsonObject {
                                                        put("emoji", status["emojiHTML"]!!)
                                                        put("message", status["message"]!!)
                                                        put("expires_at", status["expiresAt"]?.jsonPrimitive?.content)
                                                    }
                                                )
                                            } else {
                                                put("status", null as String?)
                                            }

                                            put("has_sponsor_listing", sponsorEntity["has_sponsor_listing"]?.jsonPrimitive?.contentOrNull)
                                            put("twitter_handle", sponsorEntity["twitter_handle"]?.jsonPrimitive?.contentOrNull)
                                            put("website_url", sponsorEntity["website_url"]?.jsonPrimitive?.contentOrNull)
                                            put("avatar_url", sponsorEntity["avatar_url"]?.jsonPrimitive?.contentOrNull)
                                            put("company", sponsorEntity["company"]?.jsonPrimitive?.contentOrNull)
                                            put("login", sponsorEntity["login"]?.jsonPrimitive?.contentOrNull)
                                            put("name", sponsorEntity["name"]?.jsonPrimitive?.contentOrNull)
                                            put("bio", sponsorEntity["bio"]?.jsonPrimitive?.contentOrNull)
                                        }
                                    )
                                }
                            }
                        )
                    }
                )

                put(
                    "sponsors",
                    buildJsonObject {
                        put("total_count", sponsors["totalCount"]!!)
                        put(
                            "data",
                            buildJsonArray {
                                val sponsorData = sponsors["nodes"]!!.jsonArray.toList().map { it.jsonObject }

                                for (obj in sponsorData) {
                                    val tier = obj["tier"]!!.jsonObject
                                    add(
                                        buildJsonObject {
                                            put("joined_at", obj["createdAt"]!!)
                                            put("tier_selected_at", obj["tierSelectedAt"]!!)
                                            put(
                                                "tier",
                                                buildJsonObject {
                                                    put("custom_amount", tier["isCustomAmount"]!!)
                                                    put("created_at", tier["createdAt"]!!)
                                                    put("name", tier["name"]!!)
                                                    put(
                                                        "price",
                                                        if (pricing == "Dollars") {
                                                            tier["monthlyPriceInDollars"]!!
                                                        } else {
                                                            tier["monthlyPriceInCents"]!!
                                                        }
                                                    )
                                                }
                                            )

                                            val sponsorEntity = obj["sponsorEntity"]!!.jsonObject

                                            put("followers", sponsorEntity["following"]!!)
                                            put("followers", sponsorEntity["followers"]!!)

                                            val status = try {
                                                sponsorEntity["status"]?.jsonObject
                                            } catch (e: IllegalArgumentException) {
                                                null
                                            }

                                            if (status != null) {
                                                put(
                                                    "status",
                                                    buildJsonObject {
                                                        put("emoji", status["emojiHTML"]!!)
                                                        put("message", status["message"]!!)
                                                        put("expires_at", status["expiresAt"]?.jsonPrimitive?.content)
                                                    }
                                                )
                                            } else {
                                                put("status", JsonNull)
                                            }

                                            put("has_sponsor_listing", sponsorEntity["has_sponsor_listing"]?.jsonPrimitive?.contentOrNull)
                                            put("twitter_handle", sponsorEntity["twitter_handle"]?.jsonPrimitive?.contentOrNull)
                                            put("website_url", sponsorEntity["website_url"]?.jsonPrimitive?.contentOrNull)
                                            put("avatar_url", sponsorEntity["avatar_url"]?.jsonPrimitive?.contentOrNull)
                                            put("company", sponsorEntity["company"]?.jsonPrimitive?.contentOrNull)
                                            put("login", sponsorEntity["login"]?.jsonPrimitive?.contentOrNull)
                                            put("name", sponsorEntity["name"]?.jsonPrimitive?.contentOrNull)
                                            put("bio", sponsorEntity["bio"]?.jsonPrimitive?.contentOrNull)
                                        }
                                    )
                                }
                            }
                        )
                    }
                )
            }
        )
    }
}
