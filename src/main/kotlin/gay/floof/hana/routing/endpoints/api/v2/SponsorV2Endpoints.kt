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

import gay.floof.hana.core.extensions.put
import gay.floof.hana.data.HanaConfig
import gay.floof.hana.data.types.v2.GitHubGraphQLResult
import gay.floof.hana.routing.AbstractEndpoint
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.response.*
import kotlinx.serialization.json.*

private val EMOJI_REGEX = "<div><g-emoji class=\\\\\"[\\w-]+\\\\\" alias=\\\\\"(?<alias>[\\w-]+)\\\\\" fallback-src=\\\\\"(?<fallbackSrc>[\\d\\D:\\\\]+)\\\\\">(?<emoji>.+)<\\/g-emoji>".toRegex()

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

        val sponsorData = res.receive<GitHubGraphQLResult>()
        val sponsors = sponsorData.data.user.sponsorshipsAsSponsor.nodes.map {
            buildJsonObject {
                put(
                    "tier",
                    buildJsonObject {
                        put("joined_at", it.tier.createdAt)
                        put("tier_selected_at", it.tier.tierSelectedAt)
                        put(
                            "tier",
                            buildJsonObject {
                                put("custom_amount", it.tier.isCustomAmount)
                                put("created_at", it.tier.createdAt)
                                put("price", if (pricing == "Dollars") it.tier.monthlyPriceInDollars!! else it.tier.monthlyPriceInCents ?: 0)
                            }
                        )
                    }
                )

                if (it.sponsorable.status != null) {
                    put(
                        "status",
                        buildJsonObject {
                            put("emoji", it.sponsorable.status.emojiHTML)
                            put("message", it.sponsorable.status.message)
                            put("expires_at", it.sponsorable.status.expiresAt)
                        }
                    )
                } else {
                    put("status", JsonNull)
                }

                put("has_sponsor_listing", it.sponsorable.hasSponsorsListing)
                put("twitter_handle", it.sponsorable.twitterUsername)
                put("followers", it.sponsorable.followers.totalCount)
                put("following", it.sponsorable.following.totalCount)
                put("website_url", it.sponsorable.websiteUrl)
                put("avatar_url", it.sponsorable.avatarUrl)
                put("company", it.sponsorable.company)
                put("login", it.sponsorable.login)
                put("name", it.sponsorable.name)
                put("bio", it.sponsorable.bio)
            }
        }

        val sponsoring = sponsorData.data.user.sponsorshipsAsMaintainer.nodes.map {
            buildJsonObject {
                put(
                    "tier",
                    buildJsonObject {
                        put("joined_at", it.tier.createdAt)
                        put("tier_selected_at", it.tier.tierSelectedAt)
                        put(
                            "tier",
                            buildJsonObject {
                                put("custom_amount", it.tier.isCustomAmount)
                                put("created_at", it.tier.createdAt)
                                put("price", if (pricing == "Dollars") it.tier.monthlyPriceInDollars!! else it.tier.monthlyPriceInCents ?: 0)
                            }
                        )
                    }
                )

                if (it.sponsorEntity.status != null) {
                    put(
                        "status",
                        buildJsonObject {
                            put("emoji", it.sponsorEntity.status.emojiHTML)
                            put("message", it.sponsorEntity.status.message)
                            put("expires_at", it.sponsorEntity.status.expiresAt)
                        }
                    )
                } else {
                    put("status", JsonNull)
                }

                put("has_sponsor_listing", it.sponsorEntity.hasSponsorsListing)
                put("twitter_handle", it.sponsorEntity.twitterUsername)
                put("followers", it.sponsorEntity.followers.totalCount)
                put("following", it.sponsorEntity.following.totalCount)
                put("website_url", it.sponsorEntity.websiteUrl)
                put("avatar_url", it.sponsorEntity.avatarUrl)
                put("company", it.sponsorEntity.company)
                put("login", it.sponsorEntity.login)
                put("name", it.sponsorEntity.name)
            }
        }

        call.respond(
            buildJsonObject {
                put(
                    "sponsors",
                    buildJsonObject {
                        put("total_count", sponsorData.data.user.sponsorshipsAsSponsor.totalCount)
                        put(
                            "data",
                            buildJsonArray {
                                for (node in sponsors) {
                                    add(node)
                                }
                            }
                        )
                    }
                )

                put(
                    "sponsoring",
                    buildJsonObject {
                        put("total_count", sponsorData.data.user.sponsorshipsAsMaintainer.totalCount)
                        put(
                            "data",
                            buildJsonArray {
                                for (node in sponsoring) {
                                    add(node)
                                }
                            }
                        )
                    }
                )
            }
        )
    }
}
