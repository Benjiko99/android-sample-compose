package uno.lux.sample.data.post

import java.time.Instant
import uno.lux.sample.data.SampleComments
import uno.lux.sample.data.user.User

class LocalCommentDataSource(
    private val currentUser: User,
    private val initial: Map<PostId, List<Comment>> = SampleComments,
) : CommentDataSource {

    override suspend fun loadComments(postId: PostId): List<Comment> =
        initial[postId].orEmpty()

    override suspend fun addComment(postId: PostId, text: String): Comment =
        Comment(
            id = "local-${System.nanoTime()}",
            author = currentUser,
            createdAt = Instant.now(),
            text = text,
            likeCount = 0,
        )

    override suspend fun toggleLike(postId: PostId, comment: Comment): Comment {
        val liked = !comment.isLiked
        return comment.copy(isLiked = liked, likeCount = comment.likeCount + if (liked) 1 else -1)
    }
}
