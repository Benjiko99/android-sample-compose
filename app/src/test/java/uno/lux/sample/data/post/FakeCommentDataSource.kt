package uno.lux.sample.data.post

import uno.lux.sample.data.user.User
import java.time.Instant

internal class FakeCommentDataSource(
    private val currentUser: User,
    private val comments: Map<PostId, List<Comment>> = emptyMap(),
) : CommentDataSource {

    override suspend fun loadComments(postId: PostId) = comments[postId].orEmpty()

    override suspend fun addComment(postId: PostId, text: String) = Comment(
        id = "local-new",
        author = currentUser,
        createdAt = Instant.EPOCH,
        text = text,
        likeCount = 0,
    )

    override suspend fun toggleLike(postId: PostId, comment: Comment): Comment {
        val liked = !comment.isLiked
        return comment.copy(isLiked = liked, likeCount = comment.likeCount + if (liked) 1 else -1)
    }
}
