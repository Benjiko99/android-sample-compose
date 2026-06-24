package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.CursorPageDto
import uno.lux.sample.data.network.dto.PostFeedItemDto

/** `{ "data": [...], "page": { ... } }` — cursor-paginated list envelope. */
@Serializable
data class UserPostsResponse(
    val data: List<PostFeedItemDto>,
    val page: CursorPageDto,
)
