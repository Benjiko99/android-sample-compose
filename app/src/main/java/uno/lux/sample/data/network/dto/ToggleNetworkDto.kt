package uno.lux.sample.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LikeToggleNetworkDto(val isLiked: Boolean, val likeCount: Int)

@Serializable
data class BookmarkToggleNetworkDto(val isBookmarked: Boolean)

/** Sent as the body of POST requests that carry no domain payload. Serialises to `{}`. */
@Serializable
class EmptyBody
