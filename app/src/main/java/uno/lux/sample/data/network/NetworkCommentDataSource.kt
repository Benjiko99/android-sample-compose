package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uno.lux.sample.data.network.dto.AddCommentRequestDto
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.CommentDataSource
import uno.lux.sample.data.post.PostId

class NetworkCommentDataSource(
    private val api: MosaicApi,
) : CommentDataSource {

    override suspend fun loadComments(postId: PostId): List<Comment> = withContext(Dispatchers.IO) {
        api.getComments(postId).data.map { it.toDomain() }
    }

    override suspend fun addComment(postId: PostId, text: String): Comment = withContext(Dispatchers.IO) {
        api.addComment(postId, AddCommentRequestDto(text)).data.toDomain()
    }

    override suspend fun toggleLike(postId: PostId, comment: Comment): Comment = withContext(Dispatchers.IO) {
        val result = api.toggleCommentLike(postId, comment.id, EmptyBody()).data
        comment.copy(isLiked = result.isLiked, likeCount = result.likeCount)
    }
}
