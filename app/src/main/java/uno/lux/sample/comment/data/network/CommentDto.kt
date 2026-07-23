package uno.lux.sample.comment.data.network

import java.time.Instant
import kotlinx.serialization.Serializable
import uno.lux.sample.app.core.network.InstantSerializer
import uno.lux.sample.user.data.network.UserDto

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
