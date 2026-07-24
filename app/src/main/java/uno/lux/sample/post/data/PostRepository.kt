package uno.lux.sample.post.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uno.lux.sample.post.NewPost
import uno.lux.sample.post.Post
import uno.lux.sample.post.PostId
import uno.lux.sample.post.PostWithAuthor
import uno.lux.sample.user.data.UserRepository

/**
 * Normalized entity store for posts.
 *
 * All screens share one entity map so a like toggled in the feed is immediately visible on a
 * profile and vice versa — without duplicating mutation logic. [ingest] merges posts fetched by
 * any screen into the shared store; [toggleLike] and [toggleBookmark] mutate individual entries
 * so every observer sees the update in the same emission.
 *
 * Ordered, screen-specific lists of post IDs (feed order, per-user order, etc.) live in
 * [uno.lux.sample.feed.data.FeedRepository] or [uno.lux.sample.profile.data.ProfileRepository]; this store owns only the entity data.
 *
 * The two endpoints that answer with a *single* post embed its author, so [load] and [create]
 * seed [userRepository] with it — the same thing [uno.lux.sample.profile.data.ProfileRepository] does for the authors that
 * ride along with a page of saved or liked posts. Sideloaded users belong in the user store
 * wherever they arrive from, rather than each caller remembering to put them there.
 */
class PostRepository(
    private val dataSource: PostDataSource,
    private val userRepository: UserRepository,
) {
    private val _entities = MutableStateFlow<Map<PostId, Post>>(emptyMap())
    val entities: StateFlow<Map<PostId, Post>> = _entities.asStateFlow()

    private val _deletedIds = MutableStateFlow<Set<PostId>>(emptySet())

    /**
     * IDs this session has deleted. Exists because absence from [entities] is ambiguous — a post
     * can be absent because nothing fetched it *or* because it is gone — and the one observer the
     * ambiguity bites (a detail page open on the post someone deletes from another screen) can't
     * resolve it alone. Grows only as fast as the user deletes, so it is never trimmed.
     */
    val deletedIds: StateFlow<Set<PostId>> = _deletedIds.asStateFlow()

    fun ingest(posts: List<Post>) {
        _entities.update { current -> current + posts.associateBy { it.id } }
    }

    /**
     * Fetches a single post into the store, returning it — or null when the server says there is
     * no such post, so a caller can render "gone" rather than a retryable failure.
     *
     * A screen reached the ordinary way never needs this: it opens from a feed or profile whose
     * fetch already filled the store. It exists for the screen that is the *first* thing to ask
     * for a post — a post detail page restored after process death, whose stores start empty,
     * which is also why the embedded author is seeded here rather than fetched separately.
     */
    suspend fun load(postId: PostId): Post? {
        // A deletion this session performed is an answer the store already has — no request.
        if (postId in _deletedIds.value) return null

        return dataSource.fetch(postId)?.let(::store)
    }

    /**
     * Publishes [draft] and stores the resulting entity, so any screen already resolving that ID
     * renders it without a re-fetch. Placing the post in an ordered list is [uno.lux.sample.feed.data.FeedRepository]'s
     * job, not this store's.
     */
    suspend fun create(draft: NewPost): Post = store(dataSource.create(draft))

    /** Puts a single post's two halves where each belongs, and hands back the post. */
    private fun store(fetched: PostWithAuthor): Post {
        _entities.update { it + (fetched.post.id to fetched.post) }
        userRepository.ingest(listOf(fetched.author))

        return fetched.post
    }

    /**
     * Deletes [postId] and drops it from the store. The ordered ID lists in [uno.lux.sample.feed.data.FeedRepository] and
     * [uno.lux.sample.profile.data.ProfileRepository] are deliberately left alone: they resolve IDs through [entities], so a
     * removed entity disappears from every screen showing it in the same emission — the same
     * propagation a like toggle relies on, rather than a second place that has to be kept in step.
     */
    suspend fun delete(postId: PostId) {
        dataSource.delete(postId)
        // Marked deleted *before* the entity drops, so no observer ever sees the post absent
        // without [deletedIds] already explaining why.
        _deletedIds.update { it + postId }
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
