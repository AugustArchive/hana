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

package gay.floof.hana.data.types

import kotlinx.datetime.Instant

@kotlinx.serialization.Serializable
data class GitHubGraphQLResult(
    val data: GitHubSponsorDataResult
)

@kotlinx.serialization.Serializable
data class GitHubSponsorDataResult(
    val user: GitHubUserSponsorResult
)

@kotlinx.serialization.Serializable
data class GitHubUserSponsorResult(
    val sponsorshipsAsMaintainer: SponsorshipInfo,
    val sponsorshipsAsSponsor: SponsorshipSponsorInfo
)

@kotlinx.serialization.Serializable
data class SponsorshipInfo(
    val totalCount: Int,
    val pageInfo: SponsorPageInfo,
    val nodes: List<SponsorNode>
)

@kotlinx.serialization.Serializable
data class SponsorshipSponsorInfo(
    val totalCount: Int,
    val pageInfo: SponsorPageInfo,
    val nodes: List<SponsorSponsorNode>
)

@kotlinx.serialization.Serializable
data class SponsorPageInfo(
    val hasNextPage: Boolean,
    val endCursor: String
)

@kotlinx.serialization.Serializable
data class SponsorNode(
    val createdAt: Instant,
    val tierSelectedAt: Instant? = null,
    val isOneTimePayment: Boolean,
    val tier: SponsorTierNode,
    val sponsorEntity: SponsorEntity
)

@kotlinx.serialization.Serializable
data class SponsorSponsorNode(
    val createdAt: Instant,
    val tierSelectedAt: Instant? = null,
    val isOneTimePayment: Boolean,
    val tier: SponsorTierNode,
    val sponsorable: SponsorEntity
)

@kotlinx.serialization.Serializable
data class SponsorTierNode(
    val createdAt: Instant,
    val description: String = "",
    val isCustomAmount: Boolean,
    val isOneTime: Boolean,
    val name: String,
    val monthlyPriceInCents: Int? = null,
    val monthlyPriceInDollars: Int? = null
)

@kotlinx.serialization.Serializable
data class SponsorEntity(
    val avatarUrl: String,
    val status: SponsorStatusNode,
    val followers: TotalCountNode,
    val following: TotalCountNode,
    val hasSponsorsListing: Boolean = false,
    val isSiteAdmin: Boolean,
    val isEmployee: Boolean,
    val isHireable: Boolean,
    val twitterUsername: String? = null,
    val websiteUrl: String? = null,
    val company: String = "",
    val createdAt: Instant,
    val name: String = "(unknown)",
    val bio: String = "*user is a mystery* :ghost:",
    val login: String
)

@kotlinx.serialization.Serializable
data class TotalCountNode(
    val totalCount: Int
)

@kotlinx.serialization.Serializable
data class SponsorStatusNode(
    val message: String = "",
    val expiresAt: Instant? = null,
    val emoji: Instant? = null
)
