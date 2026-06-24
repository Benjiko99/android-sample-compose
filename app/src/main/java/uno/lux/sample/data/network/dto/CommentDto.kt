package uno.lux.sample.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommentDto(
    val id: String,
    val text: String,
    val createdAt: String,
    val likeCount: Int,
    val isLiked: Boolean,
    val author: UserDto,
)

@Serializable
data class AddCommentRequestDto(val text: String)
