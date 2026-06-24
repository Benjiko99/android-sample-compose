package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uno.lux.sample.data.network.dto.AddCommentRequestDto
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.CommentRepository
import uno.lux.sample.data.post.PostId

/**
 * Network-backed [CommentRepository]. All operations are stateless: the caller owns the comment
 * list and applies the returned value. [loadComments] fetches the thread; [addComment] and
 * [toggleLike] call the API and return the server-confirmed result for the caller to apply.
 */
class NetworkCommentRepository(
    private val api: MosaicApi,
) : CommentRepository {

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
