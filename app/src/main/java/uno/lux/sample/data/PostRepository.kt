package uno.lux.sample.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Single source of truth for feed posts.
 *
 * Consumers observe [posts] reactively and express user intent through the toggle
 * functions; the repository owns every mutation so all observers stay consistent. The
 * interface is the seam a real network- or database-backed implementation — and the
 * dependency injection that would provide it — slots into later.
 */
interface PostRepository {
    val posts: Flow<List<Post>>

    /** Re-fetches the latest feed, suspending until the fetch completes. */
    suspend fun refresh()

    suspend fun toggleLike(postId: String)
    suspend fun toggleBookmark(postId: String)
}

/**
 * In-memory [PostRepository] seeded with [SamplePosts]. State lives in a [MutableStateFlow]
 * so toggles emit a fresh, immutable list to every collector.
 */
class InMemoryPostRepository(
    initialPosts: List<Post> = SamplePosts,
) : PostRepository {

    private val state = MutableStateFlow(initialPosts)
    override val posts: Flow<List<Post>> = state.asStateFlow()

    override suspend fun refresh() {
        // The in-memory feed is already current; a network-backed implementation would
        // fetch here and replace [state]. We model only the latency such a fetch incurs.
        delay(REFRESH_DELAY_MILLIS)
    }

    override suspend fun toggleLike(postId: String) = state.updatePost(postId) { post ->
        val liked = !post.isLiked
        post.copy(
            isLiked = liked,
            likeCount = post.likeCount + if (liked) 1 else -1,
        )
    }

    override suspend fun toggleBookmark(postId: String) = state.updatePost(postId) { post ->
        post.copy(isBookmarked = !post.isBookmarked)
    }

    private companion object {
        const val REFRESH_DELAY_MILLIS = 800L
    }
}

/** Replaces the single post matching [postId], leaving the rest of the list untouched. */
private inline fun MutableStateFlow<List<Post>>.updatePost(
    postId: String,
    transform: (Post) -> Post,
) = update { posts ->
    posts.map { post -> if (post.id == postId) transform(post) else post }
}
