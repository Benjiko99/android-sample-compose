package uno.lux.sample.data.post

interface CommentDataSource {
    suspend fun loadComments(postId: PostId): List<Comment>
    suspend fun addComment(postId: PostId, text: String): Comment
    suspend fun toggleLike(postId: PostId, comment: Comment): Comment
}
