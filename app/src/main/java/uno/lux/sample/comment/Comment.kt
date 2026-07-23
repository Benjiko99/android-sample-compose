package uno.lux.sample.comment

import java.time.Instant
import uno.lux.sample.user.User

typealias CommentId = String

data class Comment(
    val id: CommentId,
    val author: User,
    val createdAt: Instant,
    val text: String,
    val likeCount: Int,
    val isLiked: Boolean = false,
)
