package uno.lux.sample.comment.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.app.core.network.CursorPageDto

@Serializable
data class CommentListResponse(
    val data: List<CommentDto>,
    val page: CursorPageDto,
)
