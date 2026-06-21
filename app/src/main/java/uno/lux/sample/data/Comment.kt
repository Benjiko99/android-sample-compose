package uno.lux.sample.data

import java.time.Instant

data class Comment(
    val id: String,
    val author: User,
    val createdAt: Instant,
    val text: String,
    val likeCount: Int,
    val isLiked: Boolean = false,
)
