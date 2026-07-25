package uno.lux.sample.post.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uno.lux.sample.app.core.network.asPart
import uno.lux.sample.app.core.network.asTextPart
import uno.lux.sample.app.core.network.notFoundAsNull
import uno.lux.sample.post.NewPost
import uno.lux.sample.post.NewPostMedia
import uno.lux.sample.post.Post
import uno.lux.sample.post.PostId
import uno.lux.sample.post.PostWithAuthor
import uno.lux.sample.post.data.PostDataSource

class NetworkPostDataSource(
    private val api: PostApi,
) : PostDataSource {

    override suspend fun fetch(postId: PostId): PostWithAuthor? = withContext(Dispatchers.IO) {
        notFoundAsNull { api.getPost(postId) }
            ?.let { PostWithAuthorMapper.toPostWithAuthor(it.data) }
    }

    override suspend fun create(draft: NewPost): PostWithAuthor = withContext(Dispatchers.IO) {
        val media = draft.media

        val dto = api
            .createPost(
                title = draft.title.asTextPart(),
                body = draft.body.asTextPart(),
                // The trailing brackets are what make Rack collect the repeated parts into one
                // `images` array; a plain "images" name would let each part overwrite the last.
                images = (media as? NewPostMedia.Images)?.files.orEmpty().map { it.asPart("images[]") },
                video = (media as? NewPostMedia.Video)?.file?.asPart("video"),
            ).data

        PostWithAuthorMapper.toPostWithAuthor(dto)
    }

    override suspend fun delete(postId: PostId) = withContext(Dispatchers.IO) {
        api.deletePost(postId)
    }

    override suspend fun toggleLike(post: Post): Post = withContext(Dispatchers.IO) {
        val result = api.toggleLike(post.id).data
        post.copy(isLiked = result.isLiked, likeCount = result.likeCount)
    }

    override suspend fun toggleBookmark(post: Post): Post = withContext(Dispatchers.IO) {
        val result = api.toggleBookmark(post.id).data
        post.copy(isBookmarked = result.isBookmarked)
    }
}
