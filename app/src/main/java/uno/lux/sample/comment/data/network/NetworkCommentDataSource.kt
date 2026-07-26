package uno.lux.sample.comment.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uno.lux.sample.comment.data.CommentDataSource
import uno.lux.sample.comment.data.domain.Comment
import uno.lux.sample.common.data.network.SetLikeRequestDto
import uno.lux.sample.post.data.domain.PostId

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
        // The flip is worked out here rather than asked for on the wire, so a retry of the
        // request that follows repeats a state instead of repeating a flip.
        val result = api.setCommentLike(postId, comment.id, SetLikeRequestDto(!comment.isLiked)).data

        comment.copy(isLiked = result.isLiked, likeCount = result.likeCount)
    }
}
