package uno.lux.sample.comment.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.comment.data.network.CommentDto

@Serializable
data class CommentResponse(val data: CommentDto)
