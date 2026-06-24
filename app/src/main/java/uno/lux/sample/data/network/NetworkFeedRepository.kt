package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import uno.lux.sample.data.post.FeedRepository
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.PostRepository

/**
 * Network-backed [FeedRepository]. On [refresh] it fetches the first page with
 * [include=author], ingests the sideloaded authors into [userRepository] and the post entities
 * into [postRepository], then replaces [postIds] with the ordered IDs from the response.
 * [loadMore] appends subsequent pages using the cursor from the last response.
 *
 * Separating ID ownership (here) from entity ownership ([postRepository]) means the home feed
 * and a user's profile tab each maintain their own ordered ID list without sharing or
 * contaminating each other's pagination state.
 */
class NetworkFeedRepository(
    private val api: MosaicApi,
    private val postRepository: PostRepository,
    private val userRepository: NetworkUserRepository,
) : FeedRepository {

    private val _postIds = MutableStateFlow<List<PostId>>(emptyList())
    override val postIds: StateFlow<List<PostId>> = _postIds.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    override val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var nextCursor: String? = null

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        _postIds.value = fetchPage(cursor = null).map { it.id }
    }

    override suspend fun loadMore() = withContext(Dispatchers.IO) {
        if (!_hasMore.value) return@withContext
        _postIds.value += fetchPage(nextCursor).map { it.id }
    }

    private suspend fun fetchPage(cursor: String?): List<Post> {
        val response = api.getFeed(cursor = cursor, include = "author")
        userRepository.ingest(response.included?.users.orEmpty())
        val posts = response.data.map { it.toDomain() }
        postRepository.ingest(posts)
        nextCursor = response.page.nextCursor
        _hasMore.value = response.page.hasMore
        return posts
    }
}
