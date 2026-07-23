package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.post.NewPost
import uno.lux.sample.data.post.NewPostMedia
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostDataSource
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.PostWithAuthor

class NetworkPostDataSource(
    private val api: MosaicApi,
) : PostDataSource {

    override suspend fun fetch(postId: PostId): PostWithAuthor? = withContext(Dispatchers.IO) {
        notFoundAsNull { api.getPost(postId) }
            ?.let { PostWithAuthorMapper.toPostWithAuthor(it.data) }
    }

    override suspend fun create(draft: NewPost): PostWithAuthor = withContext(Dispatchers.IO) {
        val media = draft.media
        val video = media as? NewPostMedia.Video

        val dto = api.createPost(
            title = draft.title.asTextPart(),
            body = draft.body.asTextPart(),
            // The trailing brackets are what make Rack collect the repeated parts into one
            // `images` array; a plain "images" name would let each part overwrite the last.
            images = (media as? NewPostMedia.Images)?.files.orEmpty().map { it.asPart("images[]") },
            video = video?.file?.asPart("video"),
            videoDurationSeconds = video?.durationSeconds?.toString()?.asTextPart(),
        ).data

        PostWithAuthorMapper.toPostWithAuthor(dto)
    }

    override suspend fun delete(postId: PostId) = withContext(Dispatchers.IO) {
        api.deletePost(postId)
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
