package uno.lux.sample.data

import java.time.Instant

data class Post(
    val id: String,
    val author: User,
    val title: String,
    val body: String,
    val createdAt: Instant,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
)
