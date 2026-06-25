package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostDataSource

class NetworkPostDataSource(
    private val api: MosaicApi,
) : PostDataSource {

    override suspend fun toggleLike(post: Post): Post = withContext(Dispatchers.IO) {
        val result = api.toggleLike(post.id, EmptyBody()).data
        post.copy(isLiked = result.isLiked, likeCount = result.likeCount)
    }

    override suspend fun toggleBookmark(post: Post): Post = withContext(Dispatchers.IO) {
        val result = api.toggleBookmark(post.id, EmptyBody()).data
        post.copy(isBookmarked = result.isBookmarked)
    }
}
