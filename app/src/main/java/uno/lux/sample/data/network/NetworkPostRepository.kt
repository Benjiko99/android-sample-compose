package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.post.updateEntity

/**
 * Network-backed [PostRepository]. Acts as a normalized entity store: [ingest] is called by
 * [NetworkFeedRepository] and [NetworkProfileRepository] after their respective fetches, while
 * [toggleLike] and [toggleBookmark] send the mutation to the server and apply the confirmed
 * result — keeping the entity map authoritative.
 */
class NetworkPostRepository(
    private val api: MosaicApi,
) : PostRepository {

    private val _entities = MutableStateFlow<Map<PostId, Post>>(emptyMap())
    override val entities: StateFlow<Map<PostId, Post>> = _entities.asStateFlow()

    override fun ingest(posts: List<Post>) {
        _entities.value += posts.associateBy { it.id }
    }

    override suspend fun toggleLike(postId: PostId) = withContext(Dispatchers.IO) {
        val result = api.toggleLike(postId, EmptyBody()).data
        _entities.updateEntity(postId) { it.copy(isLiked = result.isLiked, likeCount = result.likeCount) }
    }

    override suspend fun toggleBookmark(postId: PostId) = withContext(Dispatchers.IO) {
        val result = api.toggleBookmark(postId, EmptyBody()).data
        _entities.updateEntity(postId) { it.copy(isBookmarked = result.isBookmarked) }
    }
}
