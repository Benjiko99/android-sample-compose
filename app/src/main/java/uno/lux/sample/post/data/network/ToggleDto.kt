package uno.lux.sample.post.data.network

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
