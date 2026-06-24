package uno.lux.sample.data.post

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uno.lux.sample.data.SamplePosts

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
interface PostRepository {
    val entities: StateFlow<Map<String, Post>>

    fun ingest(posts: List<Post>)
    suspend fun toggleLike(postId: String)
    suspend fun toggleBookmark(postId: String)
}

/**
 * In-memory [PostRepository] seeded with [SamplePosts]. All mutations preserve the identity of
 * unchanged entries so Compose skips re-composing those rows.
 */
class InMemoryPostRepository(
    initialPosts: List<Post> = SamplePosts,
) : PostRepository {

    private val _entities = MutableStateFlow(initialPosts.associateBy { it.id })
    override val entities: StateFlow<Map<String, Post>> = _entities.asStateFlow()

    override fun ingest(posts: List<Post>) {
        _entities.update { current -> current + posts.associateBy { it.id } }
    }

    override suspend fun toggleLike(postId: String) = _entities.updateEntity(postId) { post ->
        val liked = !post.isLiked
        post.copy(isLiked = liked, likeCount = post.likeCount + if (liked) 1 else -1)
    }

    override suspend fun toggleBookmark(postId: String) = _entities.updateEntity(postId) { post ->
        post.copy(isBookmarked = !post.isBookmarked)
    }
}

internal inline fun MutableStateFlow<Map<String, Post>>.updateEntity(
    postId: String,
    transform: (Post) -> Post,
) = update { entities ->
    val post = entities[postId] ?: return@update entities
    entities + (postId to transform(post))
}
