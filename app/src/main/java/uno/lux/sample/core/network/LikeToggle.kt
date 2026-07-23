package uno.lux.sample.core.network

import kotlinx.serialization.Serializable

/**
 * The answer to liking *something*. Posts and comments both have a heart and both reply with
 * this exact envelope, so it belongs to neither aggregate — bookmarking, which only posts do,
 * stays in `post`.
 */
@Serializable
data class LikeToggleDto(
    val isLiked: Boolean,
    val likeCount: Int,
)

@Serializable
data class LikeToggleResponse(val data: LikeToggleDto)
