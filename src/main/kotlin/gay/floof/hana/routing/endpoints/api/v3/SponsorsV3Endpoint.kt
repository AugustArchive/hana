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

package gay.floof.hana.routing.endpoints.api.v3

import gay.floof.hana.data.HanaConfig
import gay.floof.hana.data.types.GitHubGraphQLResult
import gay.floof.hana.routing.AbstractEndpoint
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.json.*

private fun graphqlQuery(
    login: String,
    pricing: String = "Dollars",
    after: String? = null,
    afterSponsor: String? = null
): String = """query {
    user(login: "$login") {
        sponsorshipsAsMaintainer(first: 100, after: "${after ?: ""}") {
            totalCount

            pageInfo {
                hasNextPage
                endCursor
            }

            nodes {
                createdAt
                tierSelectedAt
                isOneTimePayment

                tier {
                    createdAt
                    description
                    isCustomAmount
                    isOneTime
                    name
                    monthlyPriceIn$pricing
                }

                sponsorEntity {
                    ... on Organization {
                        avatarUrl(size: 1024)
                        description
                        websiteUrl
                        createdAt
                        isVerified
                        login
                        name
                    }

                    ... on User {
                        avatarUrl(size: 1024)

                        status {
                            message
                            expiresAt
                            emoji
                        }

                        followers(last: 100) {
                            totalCount
                        }

                        following(last: 100) {
                            totalCount
                        }

                        hasSponsorsListing
                        isSiteAdmin
                        isEmployee
                        isHireable
                        twitterUsername
                        websiteUrl
                        company
                        createdAt
                        name
                        bio
                        login
                    }
                }
            }
        }

        sponsorshipsAsSponsor(first: 100, after: "${afterSponsor ?: ""}") {
            totalCount

            pageInfo {
                hasNextPage
                endCursor
            }

            nodes {
                createdAt
                tierSelectedAt
                isOneTimePayment

                tier {
                    createdAt
                    description
                    isCustomAmount
                    isOneTime
                    name
                    monthlyPriceIn$pricing
                }

                sponsorEntity {
                    ... on Organization {
                        avatarUrl(size: 1024)

                        description
                        websiteUrl
                        createdAt
                        isVerified
                        login
                        name
                    }

                    ... on User {
                        avatarUrl(size: 1024)

                        status {
                            message
                            expiresAt
                            emoji
                        }

                        followers(last: 100) {
                            totalCount
                        }

                        following(last: 100) {
                            totalCount
                        }

                        hasSponsorsListing
                        isSiteAdmin
                        isEmployee
                        isHireable
                        twitterUsername
                        websiteUrl
                        company
                        createdAt
                        name
                        bio
                        login
                    }
                }
            }
        }
    }
}
""".trimIndent()

class SponsorsV3Endpoint: AbstractEndpoint("/v3/sponsors", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        call.respond(
            buildJsonObject {
                put("success", false)
                put(
                    "errors",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("code", "MISSING_LOGIN_PARAMETER")
                                put("message", "Missing `login` to view sponsors.")
                            }
                        )
                    }
                )
            }
        )
    }
}

class DefaultSponsorsEndpoint: AbstractEndpoint("/sponsors", HttpMethod.Get) {
    override suspend fun call(call: ApplicationCall) {
        call.respond(
            buildJsonObject {
                put("success", false)
                put(
                    "errors",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("code", "MISSING_LOGIN_PARAMETER")
                                put("message", "Missing `login` to view sponsors.")
                            }
                        )
                    }
                )
            }
        )
    }
}

class FetchSponsorsV3Endpoint(private val config: HanaConfig, private val httpClient: HttpClient): AbstractEndpoint("/v3/sponsors/{login}", HttpMethod.Get) {
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

        val data = mutableListOf<JsonObject>()
        val res: HttpResponse = httpClient.post("https://api.github.com/graphql") {
            header("Content-Type", "application/json")
            header("Authorization", "Bearer ${config.githubSecret}")

            setBody(
                buildJsonObject {
                    put("query", graphqlQuery(login, pricing))
                }
            )
        }

        val raw = res.body<JsonObject>()
        if (raw["data"]?.jsonObject == null && raw["errors"]?.jsonArray != null) {
            call.respond(
                HttpStatusCode.InternalServerError,
                buildJsonObject {
                    put("success", false)
                    put("errors", JsonArray(listOf()))
                    put("gql", raw["errors"]!!.jsonArray)
                }
            )
            return
        }

        call.respond(
            buildJsonObject {
                put("success", true)
                put(
                    "data",
                    buildJsonObject {
                        put("message", "This route is a WIP")
                    }
                )
            }
        )
    }
}

class DefaultFetchSponsorsEndpoint(private val config: HanaConfig, private val httpClient: HttpClient): AbstractEndpoint("/sponsors/{login}", HttpMethod.Get) {
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

        val sponsors = getSponsors(call, login, pricing)

        // assumed it errored, so let's not do anything :fleashed:
        if (sponsors.isEmpty()) return

        call.respond(
            buildJsonObject {
                put("success", true)
                put("data", buildJsonArray {})
            }
        )
    }

    private suspend fun getSponsors(
        call: ApplicationCall,
        login: String,
        pricing: String,
        cursor: String? = null,
        afterCursor: String? = null,
        data: MutableList<JsonObject> = mutableListOf()
    ): List<JsonObject> {
        val res: HttpResponse = httpClient.post("https://api.github.com/graphql") {
            header("Content-Type", "application/json")
            header("Authorization", "Bearer ${config.githubSecret}")

            setBody(
                buildJsonObject {
                    put("query", graphqlQuery(login, pricing, cursor, afterCursor))
                }
            )
        }

        val raw = res.body<JsonObject>()
        if (raw["data"]?.jsonObject == null && raw["errors"]?.jsonArray != null) {
            call.respond(
                HttpStatusCode.InternalServerError,
                buildJsonObject {
                    put("success", false)
                    put("errors", JsonArray(listOf()))
                    put("gql", raw["errors"]!!.jsonArray)
                }
            )

            return data
        }

        // check if we have a next page
        val data2 = res.body<GitHubGraphQLResult>()
        return data
    }
}
