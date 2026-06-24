package uno.lux.sample.data.post

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uno.lux.sample.data.SamplePosts

/**
 * Source of truth for the home feed's ordered post IDs.
 *
 * [postIds] holds the IDs in display order; consumers resolve them to [Post] objects via
 * [PostRepository.entities] so mutations (likes, bookmarks) propagate automatically without
 * re-fetching. [refresh] re-fetches the first page and replaces the current ID list.
 * [loadMore] appends the next page; [hasMore] signals whether one exists.
 */
interface FeedRepository {
    val postIds: StateFlow<List<PostId>>
    val hasMore: StateFlow<Boolean>
    suspend fun refresh()
    suspend fun loadMore()
}

/**
 * In-memory [FeedRepository]. The entity store is pre-seeded by [InMemoryPostRepository], so
 * [refresh] only models the latency a real network call would incur. There is only one page of
 * sample data, so [hasMore] is always false and [loadMore] is a no-op.
 */
class InMemoryFeedRepository(
    initialPostIds: List<PostId> = SamplePosts.map { it.id },
) : FeedRepository {

    private val _postIds = MutableStateFlow(initialPostIds)
    override val postIds: StateFlow<List<PostId>> = _postIds.asStateFlow()

    override val hasMore: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun refresh() {
        delay(REFRESH_DELAY_MILLIS)
    }

    override suspend fun loadMore() {}

    private companion object {
        const val REFRESH_DELAY_MILLIS = 800L
    }
}
