package uno.lux.sample.comment.data

import uno.lux.sample.comment.data.domain.Comment
import uno.lux.sample.comment.data.domain.CommentId
import uno.lux.sample.common.data.LikeState
import uno.lux.sample.post.data.domain.PostId
import uno.lux.sample.user.data.domain.User
import java.time.Instant

internal class FakeCommentDataSource(
    private val currentUser: User,
    /** What [loadComments] serves. A `var` so a test can change what a *re*-load brings back. */
    var comments: Map<PostId, List<Comment>> = emptyMap(),
) : CommentDataSource {

    /** Set to make [addComment] fail, for the paths that must not move on an unposted comment. */
    var addError: Throwable? = null

    override suspend fun loadComments(postId: PostId) = comments[postId].orEmpty()

    override suspend fun addComment(postId: PostId, text: String): Comment {
        addError?.let { throw it }

        return Comment(
            id = "local-new",
            author = currentUser,
            createdAt = Instant.EPOCH,
            text = text,
            likeCount = 0,
        )
    }

    /**
     * The like counts this fake's server holds, keyed by comment ID.
     *
     * A like request names a comment and a state, not a count, so the number in the answer is the
     * server's own — a test asserting on one seeds it here to say what the server started from.
     */
    val likeCounts = mutableMapOf<CommentId, Int>()

    /** The `(commentId, liked)` pairs passed to [setLike], in call order, for test assertions. */
    val likeRequests = mutableListOf<Pair<CommentId, Boolean>>()

    /** Thrown by [setLike] instead of answering, so tests can drive the failure path. */
    var setLikeError: Exception? = null

    /**
     * Runs inside [setLike] before it answers, which is the only moment a test can see the
     * optimistic value the ViewModel wrote before the request went out.
     */
    var whileInFlight: (suspend () -> Unit)? = null

    override suspend fun setLike(
        postId: PostId,
        commentId: CommentId,
        liked: Boolean,
    ): LikeState {
        likeRequests += commentId to liked
        whileInFlight?.invoke()
        setLikeError?.let { throw it }

        val settled = (likeCounts[commentId] ?: 0) + if (liked) 1 else -1
        likeCounts[commentId] = settled

        return LikeState(isLiked = liked, likeCount = settled)
    }
}
