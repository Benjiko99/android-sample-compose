package uno.lux.sample.profile.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.app.core.network.CursorPageDto
import uno.lux.sample.post.data.network.PostFeedItemDto

/** `{ "data": [...], "page": { ... } }` — cursor-paginated list envelope. */
@Serializable
data class UserPostsResponse(
    val data: List<PostFeedItemDto>,
    val page: CursorPageDto,
)
