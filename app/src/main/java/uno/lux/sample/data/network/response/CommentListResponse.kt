package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.CommentDto
import uno.lux.sample.data.network.dto.CursorPageDto

@Serializable
data class CommentListResponse(
    val data: List<CommentDto>,
    val page: CursorPageDto,
)
