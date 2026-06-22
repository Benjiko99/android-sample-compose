package uno.lux.sample.data.post

import java.time.Instant
import uno.lux.sample.data.User

data class Comment(
    val id: String,
    val author: User,
    val createdAt: Instant,
    val text: String,
    val likeCount: Int,
    val isLiked: Boolean = false,
)
