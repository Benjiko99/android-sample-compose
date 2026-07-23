package uno.lux.sample.profile.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.app.core.network.CursorPageDto
import uno.lux.sample.user.data.network.SideloadedUsers
import uno.lux.sample.post.data.network.PostFeedItemDto

/**
 * `{ "data": [...], "included": { "users": [...] }, "page": { ... } }` — the profile's saved and
 * liked posts, which share one shape. [included] is not optional the way it is on the feed: these
 * posts are by arbitrary authors, so the server always sends them.
 */
@Serializable
data class PostsWithAuthorsResponse(
    val data: List<PostFeedItemDto>,
    val included: SideloadedUsers,
    val page: CursorPageDto,
)
