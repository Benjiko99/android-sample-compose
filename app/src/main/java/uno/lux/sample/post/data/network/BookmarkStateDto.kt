package uno.lux.sample.post.data.network

import kotlinx.serialization.Serializable

/**
 * Asks for a bookmark to be *in* a state rather than to be flipped, for the same reason
 * [uno.lux.sample.common.data.network.SetLikeRequestDto] does.
 */
@Serializable
data class SetBookmarkRequestDto(
    val bookmarked: Boolean,
)

@Serializable
data class BookmarkStateDto(
    val isBookmarked: Boolean,
)
