package uno.lux.sample.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * Single source of truth for post comments.
 *
 * Consumers observe [comments] reactively and express intent through the mutating functions.
 * The interface is the seam a real network-backed implementation slots into later.
 */
interface CommentRepository {
    fun comments(postId: String): Flow<List<Comment>>
    suspend fun addComment(postId: String, text: String)
    suspend fun toggleLike(postId: String, commentId: String)
}

/**
 * In-memory [CommentRepository] seeded with [SampleComments]. New comments prepend to the list
 * so they appear at the top — matches the "freshest first" ordering in the detail screen.
 */
class InMemoryCommentRepository(
    private val currentUser: User,
    initial: Map<String, List<Comment>> = SampleComments,
) : CommentRepository {

    private val state = MutableStateFlow(initial)

    override fun comments(postId: String): Flow<List<Comment>> =
        state.map { it[postId] ?: emptyList() }

    override suspend fun addComment(postId: String, text: String) {
        val comment = Comment(
            id = "local-${System.nanoTime()}",
            author = currentUser,
            createdAt = Instant.now(),
            text = text,
            likeCount = 0,
        )
        state.update { map ->
            map + (postId to (listOf(comment) + (map[postId] ?: emptyList())))
        }
    }

    override suspend fun toggleLike(postId: String, commentId: String) {
        state.update { map ->
            val comments = map[postId] ?: return@update map
            map + (postId to comments.map { c ->
                if (c.id != commentId) c
                else c.copy(
                    isLiked = !c.isLiked,
                    likeCount = c.likeCount + if (c.isLiked) -1 else 1,
                )
            })
        }
    }
}
