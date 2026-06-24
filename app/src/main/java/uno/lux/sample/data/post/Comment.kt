package uno.lux.sample.data.post

import java.time.Instant
import uno.lux.sample.data.user.User

typealias CommentId = String

data class Comment(
    val id: CommentId,
    val author: User,
    val createdAt: Instant,
    val text: String,
    val likeCount: Int,
    val isLiked: Boolean = false,
)
