package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import uno.lux.sample.data.post.FeedRepository
import uno.lux.sample.data.post.PostRepository

/**
 * Network-backed [FeedRepository]. On [refresh] it fetches the first page with
 * [include=author], ingests the sideloaded authors into [userRepository] and the post entities
 * into [postRepository], then replaces [postIds] with the ordered IDs from the response.
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

    private val _postIds = MutableStateFlow<List<String>>(emptyList())
    override val postIds: StateFlow<List<String>> = _postIds.asStateFlow()

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        val response = api.getFeed(include = "author")
        userRepository.ingest(response.included?.users.orEmpty())
        val posts = response.data.map { it.toDomain() }
        postRepository.ingest(posts)
        _postIds.value = posts.map { it.id }
    }
}
