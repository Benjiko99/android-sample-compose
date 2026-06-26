package uno.lux.sample.data.post

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uno.lux.sample.data.user.UserRepository

/**
 * Pagination state for the home feed. Starts as [NotLoaded] until the first [FeedRepository.refresh]
 * completes; transitions to [Loaded] atomically with the ingested post and user data, so the ViewModel's
 * combine never sees an inconsistent "loaded flag but no entities yet" intermediate state.
 */
sealed interface FeedState {
    data object NotLoaded : FeedState

    data class Loaded(
        val postIds: List<PostId>,
        val hasMore: Boolean
    ) : FeedState
}

/**
 * Source of truth for the home feed's ordered post IDs.
 *
 * [feedState] carries the IDs in display order plus the pagination flag; consumers resolve IDs to
 * [Post] objects via [PostRepository.entities] so mutations (likes, bookmarks) propagate automatically
 * without re-fetching. [refresh] re-fetches the first page. [loadMore] appends the next page.
 *
 * Where the data comes from — in-memory sample data vs. a live network call — is decided by
 * [FeedDataSource]. This class owns only the ordered ID list and pagination state.
 */
class FeedRepository(
    private val dataSource: FeedDataSource,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) {
    private val _feedState = MutableStateFlow<FeedState>(FeedState.NotLoaded)
    val feedState: StateFlow<FeedState> = _feedState.asStateFlow()

    private var nextCursor: String? = null

    fun reset() {
        _feedState.value = FeedState.NotLoaded
    }

    suspend fun refresh() {
        val page = dataSource.fetch(cursor = null)
        postRepository.ingest(page.posts)
        userRepository.ingest(page.users)
        nextCursor = page.nextCursor
        // Set last: entities and users are already ingested, so when this emits the combine
        // can resolve every post ID without a stale-data gap.
        _feedState.value = FeedState.Loaded(page.posts.map { it.id }, page.hasMore)
    }

    suspend fun loadMore() {
        val current = _feedState.value as? FeedState.Loaded ?: return
        if (!current.hasMore) return
        val page = dataSource.fetch(cursor = nextCursor)
        postRepository.ingest(page.posts)
        userRepository.ingest(page.users)
        nextCursor = page.nextCursor
        _feedState.value = FeedState.Loaded(
            postIds = buildList {
                addAll(current.postIds)
                page.posts.mapTo(this) { it.id }
            },
            hasMore = page.hasMore,
        )
    }
}
