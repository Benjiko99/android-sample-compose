package uno.lux.sample.data.post

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uno.lux.sample.data.user.UserRepository

/**
 * Source of truth for the home feed's ordered post IDs.
 *
 * [postIds] holds the IDs in display order; consumers resolve them to [Post] objects via
 * [PostRepository.entities] so mutations (likes, bookmarks) propagate automatically without
 * re-fetching. [refresh] re-fetches the first page and replaces the current ID list.
 * [loadMore] appends the next page; [hasMore] signals whether one exists.
 *
 * Where the data comes from — in-memory sample data vs. a live network call — is decided by
 * [FeedDataSource]. This class owns only the ordered ID list and pagination state.
 */
class FeedRepository(
    private val dataSource: FeedDataSource,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) {
    private val _postIds = MutableStateFlow<List<PostId>>(emptyList())
    val postIds: StateFlow<List<PostId>> = _postIds.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var nextCursor: String? = null

    suspend fun refresh() {
        val page = dataSource.fetch(cursor = null)
        postRepository.ingest(page.posts)
        userRepository.ingest(page.users)
        nextCursor = page.nextCursor
        _hasMore.value = page.hasMore
        _postIds.value = page.posts.map { it.id }
    }

    suspend fun loadMore() {
        if (!_hasMore.value) return
        val page = dataSource.fetch(cursor = nextCursor)
        postRepository.ingest(page.posts)
        userRepository.ingest(page.users)
        nextCursor = page.nextCursor
        _hasMore.value = page.hasMore
        _postIds.value = _postIds.value + page.posts.map { it.id }
    }
}
