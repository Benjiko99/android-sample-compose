package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uno.lux.sample.data.network.dto.CreatePostRequestDto
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.post.CreatedPost
import uno.lux.sample.data.post.NewPost
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostDataSource

class NetworkPostDataSource(
    private val api: MosaicApi,
) : PostDataSource {

    override suspend fun create(draft: NewPost): CreatedPost = withContext(Dispatchers.IO) {
        val dto = api.createPost(CreatePostRequestDto(title = draft.title, body = draft.body)).data
        CreatedPostMapper.toCreatedPost(dto)
    }

    override suspend fun toggleLike(post: Post): Post = withContext(Dispatchers.IO) {
        val result = api.toggleLike(post.id, EmptyBody()).data
        post.copy(isLiked = result.isLiked, likeCount = result.likeCount)
    }

    override suspend fun toggleBookmark(post: Post): Post = withContext(Dispatchers.IO) {
        val result = api.toggleBookmark(post.id, EmptyBody()).data
        post.copy(isBookmarked = result.isBookmarked)
    }
}
