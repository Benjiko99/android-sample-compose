package uno.lux.sample.post

import uno.lux.sample.album.Album
import java.time.Instant
import uno.lux.sample.user.User
import uno.lux.sample.user.UserId
import uno.lux.sample.video.Video

typealias PostId = String

data class Post(
    val id: PostId,
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

/**
 * A single post together with the [author] the server embedded alongside it — what the full post
 * projection carries, as opposed to the feed's `authorId` reference. Both travel together so a
 * caller can seed the post and user stores in one step, which is what the two endpoints returning
 * this shape are for: publishing a post, and fetching one the app doesn't hold yet.
 */
data class PostWithAuthor(
    val post: Post,
    val author: User,
)
