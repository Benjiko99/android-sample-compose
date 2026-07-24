package uno.lux.sample.comment.data

import uno.lux.sample.comment.Comment
import uno.lux.sample.post.PostId

/**
 * Stateless data source for post comments.
 *
 * All three operations return values; the caller ([PostDetailViewModel]) owns the comment list
 * state and applies the returned value. This keeps comments scoped to the screen that needs them
 * and lets the ViewModel be cleared — along with the comment data — the moment the user pops the
 * post detail screen.
 */
class CommentRepository(
    private val dataSource: CommentDataSource,
) {
    suspend fun loadComments(postId: PostId) = dataSource.loadComments(postId)

    suspend fun addComment(postId: PostId, text: String) = dataSource.addComment(postId, text)

    suspend fun toggleLike(
        postId: PostId,
        comment: Comment,
    ) = dataSource.toggleLike(postId, comment)
}
