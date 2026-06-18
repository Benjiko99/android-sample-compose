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
    /** When set, the post carries a video that plays inline in the feed. */
    val video: Video? = null,
    /** When set, the post carries an image album shown as a horizontally scrolling row. */
    val album: Album? = null,
)
