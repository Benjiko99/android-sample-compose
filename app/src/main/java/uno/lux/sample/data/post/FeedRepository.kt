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
 */
interface FeedRepository {
    val postIds: StateFlow<List<PostId>>
    suspend fun refresh()
}

/**
 * In-memory [FeedRepository]. The entity store is pre-seeded by [InMemoryPostRepository], so
 * [refresh] only models the latency a real network call would incur.
 */
class InMemoryFeedRepository(
    initialPostIds: List<PostId> = SamplePosts.map { it.id },
) : FeedRepository {

    private val _postIds = MutableStateFlow(initialPostIds)
    override val postIds: StateFlow<List<PostId>> = _postIds.asStateFlow()

    override suspend fun refresh() {
        delay(REFRESH_DELAY_MILLIS)
    }

    private companion object {
        const val REFRESH_DELAY_MILLIS = 800L
    }
}
