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
