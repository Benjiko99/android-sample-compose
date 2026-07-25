package uno.lux.sample.comment.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.common.data.network.CursorPageDto

@Serializable
data class CommentListResponse(
    val data: List<CommentDto>,
    val page: CursorPageDto,
)
