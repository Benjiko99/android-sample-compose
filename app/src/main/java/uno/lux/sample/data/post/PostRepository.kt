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
    val entities: StateFlow<Map<PostId, Post>>

    fun ingest(posts: List<Post>)
    suspend fun toggleLike(postId: PostId)
    suspend fun toggleBookmark(postId: PostId)
}

// TODO: Move the InMemory implementations of repositories out of the file's that declare their interface
//  and into their own package, like we have for network implementations.
// TODO: It would be better if InMemory repositories didn't even exist, and instead we have
//  DataSource classes that change where the data is coming from, so we don't have to duplicate
//  the repository implementations, and so tests run against the real implementation of the repository
//  that will be used in production.
//  We're also duplicating tests for Network and InMemory repository classes currently, another
//  bad thing.
/**
 * In-memory [PostRepository] seeded with [SamplePosts]. All mutations preserve the identity of
 * unchanged entries so Compose skips re-composing those rows.
 */
class InMemoryPostRepository(
    initialPosts: List<Post> = SamplePosts,
) : PostRepository {

    private val _entities = MutableStateFlow(initialPosts.associateBy { it.id })
    override val entities: StateFlow<Map<PostId, Post>> = _entities.asStateFlow()

    override fun ingest(posts: List<Post>) {
        _entities.update { current -> current + posts.associateBy { it.id } }
    }

    override suspend fun toggleLike(postId: PostId) = _entities.updateEntity(postId) { post ->
        val liked = !post.isLiked
        post.copy(isLiked = liked, likeCount = post.likeCount + if (liked) 1 else -1)
    }

    override suspend fun toggleBookmark(postId: PostId) = _entities.updateEntity(postId) { post ->
        post.copy(isBookmarked = !post.isBookmarked)
    }
}

internal inline fun MutableStateFlow<Map<PostId, Post>>.updateEntity(
    postId: PostId,
    transform: (Post) -> Post,
) = update { entities ->
    val post = entities[postId] ?: return@update entities
    entities + (postId to transform(post))
}
