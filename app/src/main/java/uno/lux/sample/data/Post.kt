package uno.lux.sample.data

import java.time.Instant

/**
 * A text post in the feed.
 *
 * [isLiked] and [isBookmarked] are per-viewer state that the feed toggles; [likeCount]
 * already accounts for the viewer's own like so the UI never has to reconcile the two.
 */
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
