package uno.lux.sample.data.post

import java.time.Instant
import uno.lux.sample.data.user.UserId

typealias PostId = String

data class Post(
    val id: PostId,
    /** The canonical shareable link, built by the server — never assembled from [id] here. */
    val url: String,
    val authorId: UserId,
    val title: String,
    val body: String,
    val createdAt: Instant,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val video: Video? = null,
    val album: Album? = null,
)
