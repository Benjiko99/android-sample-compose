package uno.lux.sample.feed.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.core.network.CursorPageDto
import uno.lux.sample.post.data.network.PostFeedItemDto
import uno.lux.sample.user.data.network.SideloadedUsers
import uno.lux.sample.user.data.network.UserDto

/** Top-level feed response — not wrapped in an extra `data` key. */
@Serializable
data class FeedResponse(
    val data: List<PostFeedItemDto>,
    val included: SideloadedUsers? = null,
    val page: CursorPageDto,
)
