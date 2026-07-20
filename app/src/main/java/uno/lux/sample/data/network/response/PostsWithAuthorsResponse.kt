package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.CursorPageDto
import uno.lux.sample.data.network.dto.PostFeedItemDto

/**
 * `{ "data": [...], "included": { "users": [...] }, "page": { ... } }` — the profile's saved and
 * liked posts, which share one shape. [included] is not optional the way it is on the feed: these
 * posts are by arbitrary authors, so the server always sends them.
 */
@Serializable
data class PostsWithAuthorsResponse(
    val data: List<PostFeedItemDto>,
    val included: FeedIncluded,
    val page: CursorPageDto,
)
