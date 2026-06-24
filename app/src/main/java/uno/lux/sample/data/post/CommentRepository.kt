package uno.lux.sample.data.post

import java.time.Instant
import uno.lux.sample.data.SampleComments
import uno.lux.sample.data.user.User

/**
 * Stateless data source for post comments.
 *
 * All three operations return values; the caller ([PostDetailViewModel]) owns the comment list
 * state and applies the returned value. This keeps comments scoped to the screen that needs them
 * and lets the ViewModel be cleared — along with the comment data — the moment the user pops the
 * post detail screen.
 */
interface CommentRepository {
    suspend fun loadComments(postId: String): List<Comment>
    suspend fun addComment(postId: String, text: String): Comment
    suspend fun toggleLike(postId: String, comment: Comment): Comment
}

/** In-memory [CommentRepository] seeded from [SampleComments]. */
class InMemoryCommentRepository(
    private val currentUser: User,
    private val initial: Map<String, List<Comment>> = SampleComments,
) : CommentRepository {

    override suspend fun loadComments(postId: String): List<Comment> =
        initial[postId].orEmpty()

    override suspend fun addComment(postId: String, text: String): Comment =
        Comment(
            id = "local-${System.nanoTime()}",
            author = currentUser,
            createdAt = Instant.now(),
            text = text,
            likeCount = 0,
        )

    override suspend fun toggleLike(postId: String, comment: Comment): Comment {
        val liked = !comment.isLiked
        return comment.copy(isLiked = liked, likeCount = comment.likeCount + if (liked) 1 else -1)
    }
}
