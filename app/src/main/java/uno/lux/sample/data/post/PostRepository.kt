package uno.lux.sample.data.post

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uno.lux.sample.data.profile.ProfileRepository

/**
 * Normalized entity store for posts.
 *
 * All screens share one entity map so a like toggled in the feed is immediately visible on a
 * profile and vice versa — without duplicating mutation logic. [ingest] merges posts fetched by
 * any screen into the shared store; [toggleLike] and [toggleBookmark] mutate individual entries
 * so every observer sees the update in the same emission.
 *
 * Ordered, screen-specific lists of post IDs (feed order, per-user order, etc.) live in
 * [FeedRepository] or [ProfileRepository]; this store owns only the entity data.
 */
class PostRepository(
    private val dataSource: PostDataSource,
) {
    private val _entities = MutableStateFlow<Map<PostId, Post>>(emptyMap())
    val entities: StateFlow<Map<PostId, Post>> = _entities.asStateFlow()

    fun ingest(posts: List<Post>) {
        _entities.update { current -> current + posts.associateBy { it.id } }
    }

    suspend fun toggleLike(postId: PostId) {
        val post = _entities.value[postId] ?: return
        val updated = dataSource.toggleLike(post)
        _entities.update { it + (postId to updated) }
    }

    suspend fun toggleBookmark(postId: PostId) {
        val post = _entities.value[postId] ?: return
        val updated = dataSource.toggleBookmark(post)
        _entities.update { it + (postId to updated) }
    }
}
