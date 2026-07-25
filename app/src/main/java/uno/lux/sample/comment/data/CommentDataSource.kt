package uno.lux.sample.comment.data

import uno.lux.sample.comment.data.domain.Comment
import uno.lux.sample.post.data.domain.PostId

interface CommentDataSource {
    suspend fun loadComments(postId: PostId): List<Comment>

    suspend fun addComment(postId: PostId, text: String): Comment

    suspend fun toggleLike(postId: PostId, comment: Comment): Comment
}
