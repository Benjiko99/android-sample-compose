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

    /**
     * Fetches a single post into the store, returning it with its author — or null when the server
     * says there is no such post, so a caller can render "gone" rather than a retryable failure.
     * As with [create], the author travels back to the caller to ingest rather than being written
     * here: this store owns post entities only.
     *
     * A screen reached the ordinary way never needs this — it opens from a feed or profile whose
     * fetch already filled the store. It exists for the screen that is the *first* thing to ask
     * for a post: a post detail page restored after process death, whose stores start empty.
     */
    suspend fun load(postId: PostId): PostWithAuthor? {
        val loaded = dataSource.fetch(postId) ?: return null
        _entities.update { it + (loaded.post.id to loaded.post) }

        return loaded
    }

    /**
     * Publishes [draft] and stores the resulting entity, so any screen already resolving that ID
     * renders it without a re-fetch. The author travels back with it for the caller to ingest —
     * placing the post in an ordered list is [FeedRepository]'s job, not this store's.
     */
    suspend fun create(draft: NewPost): PostWithAuthor {
        val created = dataSource.create(draft)
        _entities.update { it + (created.post.id to created.post) }

        return created
    }

    /**
     * Deletes [postId] and drops it from the store. The ordered ID lists in [FeedRepository] and
     * [ProfileRepository] are deliberately left alone: they resolve IDs through [entities], so a
     * removed entity disappears from every screen showing it in the same emission — the same
     * propagation a like toggle relies on, rather than a second place that has to be kept in step.
     */
    suspend fun delete(postId: PostId) {
        dataSource.delete(postId)
        _entities.update { it - postId }
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
