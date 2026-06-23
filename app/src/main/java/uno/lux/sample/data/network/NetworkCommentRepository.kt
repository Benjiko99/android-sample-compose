package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import uno.lux.sample.data.network.dto.AddCommentRequestDto
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.CommentRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Network-backed [CommentRepository]. Comment threads are loaded lazily: the first collector
 * of [comments] triggers the API call; subsequent collectors (and mutations) share the cached
 * [MutableStateFlow] per post. New comments are prepended so they appear at the top.
 */
class NetworkCommentRepository(
    private val api: MosaicApi,
) : CommentRepository {

    private val commentStates = ConcurrentHashMap<String, MutableStateFlow<List<Comment>>>()

    override fun comments(postId: String): Flow<List<Comment>> = flow {
        val fresh = MutableStateFlow<List<Comment>>(emptyList())
        val state = commentStates.putIfAbsent(postId, fresh) ?: run {
            runCatching {
                fresh.value = withContext(Dispatchers.IO) { api.getComments(postId).data.map { it.toDomain() } }
            }.onFailure {
                // Remove the (empty) entry so the next collector retries rather than being
                // permanently stuck with an empty thread.
                commentStates.remove(postId, fresh)
            }
            fresh
        }
        emitAll(state)
    }

    override suspend fun addComment(postId: String, text: String) = withContext(Dispatchers.IO) {
        val created = api.addComment(postId, AddCommentRequestDto(text)).data
        commentStates.getOrPut(postId) { MutableStateFlow(emptyList()) }.update { current ->
            listOf(created.toDomain()) + current
        }
    }

    override suspend fun toggleLike(postId: String, commentId: String) = withContext(Dispatchers.IO) {
        val result = api.toggleCommentLike(postId, commentId, EmptyBody()).data
        commentStates.getOrPut(postId) { MutableStateFlow(emptyList()) }.update { comments ->
            comments.map { c ->
                if (c.id == commentId) c.copy(isLiked = result.isLiked, likeCount = result.likeCount)
                else c
            }
        }
    }
}
