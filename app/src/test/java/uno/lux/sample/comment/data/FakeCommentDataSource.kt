package uno.lux.sample.comment.data

import uno.lux.sample.comment.data.domain.Comment
import uno.lux.sample.post.data.domain.PostId
import uno.lux.sample.user.data.domain.User
import java.time.Instant

internal class FakeCommentDataSource(
    private val currentUser: User,
    private val comments: Map<PostId, List<Comment>> = emptyMap(),
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

    override suspend fun toggleLike(postId: PostId, comment: Comment): Comment {
        val liked = !comment.isLiked
        return comment.copy(isLiked = liked, likeCount = comment.likeCount + if (liked) 1 else -1)
    }
}
