package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.CursorPageDto
import uno.lux.sample.data.network.dto.PostFeedItemDto

/**
 * `{ "data": [...], "included": { "users": [...] }, "page": { ... } }` — saved posts.
 * [included] is not optional here the way it is on the feed: saved posts are by arbitrary
 * authors, so the server always sends them.
 */
@Serializable
data class BookmarksResponse(
    val data: List<PostFeedItemDto>,
    val included: FeedIncluded,
    val page: CursorPageDto,
)
