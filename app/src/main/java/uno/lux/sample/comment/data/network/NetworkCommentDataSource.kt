package uno.lux.sample.comment.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uno.lux.sample.comment.Comment
import uno.lux.sample.comment.data.CommentDataSource
import uno.lux.sample.post.PostId
import uno.lux.sample.core.network.EmptyBody

class NetworkCommentDataSource(
    private val api: CommentApi,
) : CommentDataSource {

    override suspend fun loadComments(postId: PostId): List<Comment> = withContext(Dispatchers.IO) {
        api.getComments(postId).data.map { CommentMapper.map(it) }
    }

    override suspend fun addComment(postId: PostId, text: String): Comment = withContext(Dispatchers.IO) {
        CommentMapper.map(api.addComment(postId, AddCommentRequestDto(text)).data)
    }

    override suspend fun toggleLike(postId: PostId, comment: Comment): Comment = withContext(Dispatchers.IO) {
        val result = api.toggleCommentLike(postId, comment.id, EmptyBody()).data
        comment.copy(isLiked = result.isLiked, likeCount = result.likeCount)
    }
}
