package uno.lux.sample.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
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
    private val api: SampleApi,
) : CommentRepository {

    private val commentStates = ConcurrentHashMap<String, MutableStateFlow<List<Comment>>>()
    private val loadedPosts: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private fun commentState(postId: String): MutableStateFlow<List<Comment>> =
        commentStates.getOrPut(postId) { MutableStateFlow(emptyList()) }

    override fun comments(postId: String): Flow<List<Comment>> {
        val state = commentState(postId)
        return flow {
            if (loadedPosts.add(postId)) {
                runCatching {
                    val response = api.getComments(postId)
                    state.value = response.data.map { it.toDomain() }
                }
            }
            emitAll(state)
        }
    }

    override suspend fun addComment(postId: String, text: String) {
        val created = api.addComment(postId, AddCommentRequestDto(text)).data
        commentState(postId).update { current -> listOf(created.toDomain()) + current }
    }

    override suspend fun toggleLike(postId: String, commentId: String) {
        val result = api.toggleCommentLike(postId, commentId, EmptyBody()).data
        commentState(postId).update { comments ->
            comments.map { c ->
                if (c.id == commentId) c.copy(isLiked = result.isLiked, likeCount = result.likeCount)
                else c
            }
        }
    }
}
