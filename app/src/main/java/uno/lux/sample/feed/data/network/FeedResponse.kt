package uno.lux.sample.feed.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.common.data.network.CursorPageDto
import uno.lux.sample.post.data.network.PostDto
import uno.lux.sample.user.data.network.SideloadedUsers

/** Top-level feed response — not wrapped in an extra `data` key. */
@Serializable
data class FeedResponse(
    val data: List<PostDto>,
    val included: SideloadedUsers? = null,
    val page: CursorPageDto,
)
