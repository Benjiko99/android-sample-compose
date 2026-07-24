package uno.lux.sample.comment.data.network

import kotlinx.serialization.Serializable

@Serializable
data class CommentResponse(
    val data: CommentDto,
)
