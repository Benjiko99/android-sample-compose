package uno.lux.sample.comment.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.comment.data.network.CommentDto
import uno.lux.sample.core.network.CursorPageDto

@Serializable
data class CommentListResponse(
    val data: List<CommentDto>,
    val page: CursorPageDto,
)
