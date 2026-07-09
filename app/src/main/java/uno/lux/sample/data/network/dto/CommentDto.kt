package uno.lux.sample.data.network.dto

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class CommentDto(
    val id: String,
    val text: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val likeCount: Int,
    val isLiked: Boolean,
    val author: UserDto,
)

@Serializable
data class AddCommentRequestDto(val text: String)
