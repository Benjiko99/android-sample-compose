package uno.lux.sample.data.post

/**
 * Stateless data source for post comments.
 *
 * All three operations return values; the caller ([PostDetailViewModel]) owns the comment list
 * state and applies the returned value. This keeps comments scoped to the screen that needs them
 * and lets the ViewModel be cleared — along with the comment data — the moment the user pops the
 * post detail screen.
 *
 * Where the data comes from — in-memory sample data vs. a live network call — is decided by
 * [CommentDataSource].
 */
class CommentRepository(
    private val dataSource: CommentDataSource,
) {
    suspend fun loadComments(postId: PostId) = dataSource.loadComments(postId)
    suspend fun addComment(postId: PostId, text: String) = dataSource.addComment(postId, text)
    suspend fun toggleLike(postId: PostId, comment: Comment) = dataSource.toggleLike(postId, comment)
}
