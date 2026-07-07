package uno.lux.sample.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LikeToggleDto(
    val isLiked: Boolean,
    val likeCount: Int,
)

@Serializable
data class BookmarkToggleDto(
    val isBookmarked: Boolean,
)

@Serializable
data class FollowToggleDto(
    val isFollowing: Boolean,
    val followerCount: Int,
)

/** Sent as the body of POST requests that carry no domain payload. Serialises to `{}`. */
@Serializable
class EmptyBody
