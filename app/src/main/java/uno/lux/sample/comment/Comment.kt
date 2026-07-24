package uno.lux.sample.comment

import uno.lux.sample.user.User
import java.time.Instant

typealias CommentId = String

data class Comment(
    val id: CommentId,
    val author: User,
    val createdAt: Instant,
    val text: String,
    val likeCount: Int,
    val isLiked: Boolean = false,
)
