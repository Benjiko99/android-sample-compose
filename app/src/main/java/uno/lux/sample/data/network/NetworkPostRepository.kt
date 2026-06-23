package uno.lux.sample.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostRepository

/**
 * Network-backed [PostRepository] for the home feed. On [refresh] it fetches the first page
 * with `include=author` and pushes the sideloaded authors into [userRepository] so they are
 * immediately available to [HomeViewModel]'s combine — a single API round-trip populates both
 * the post list and the user cache.
 */
class NetworkPostRepository(
    private val api: MosaicApi,
    private val userRepository: NetworkUserRepository,
) : PostRepository {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    override val posts: Flow<List<Post>> = _posts.asStateFlow()

    override suspend fun refresh() {
        val response = api.getFeed(include = "author")
        userRepository.ingest(response.included?.users.orEmpty())
        _posts.value = response.data.map { it.toDomain() }
    }

    override suspend fun toggleLike(postId: String) {
        val result = api.toggleLike(postId, EmptyBody()).data
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) post.copy(isLiked = result.isLiked, likeCount = result.likeCount)
                else post
            }
        }
    }

    override suspend fun toggleBookmark(postId: String) {
        val result = api.toggleBookmark(postId, EmptyBody()).data
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) post.copy(isBookmarked = result.isBookmarked)
                else post
            }
        }
    }
}
