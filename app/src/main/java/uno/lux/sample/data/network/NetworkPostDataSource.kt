package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import uno.lux.sample.data.file.FileUpload
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.post.CreatedPost
import uno.lux.sample.data.post.NewPost
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostDataSource

class NetworkPostDataSource(
    private val api: MosaicApi,
) : PostDataSource {

    override suspend fun create(draft: NewPost): CreatedPost = withContext(Dispatchers.IO) {
        val dto = api.createPost(
            title = textPart(draft.title),
            body = textPart(draft.body),
            images = draft.images.map(::imagePart),
        ).data

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

    // The trailing brackets are what make Rack collect the repeated parts into one `images`
    // array server-side; a plain "images" name would let each part overwrite the last.
    private fun imagePart(image: FileUpload) = MultipartBody.Part.createFormData(
        name = "images[]",
        filename = image.filename,
        body = image.bytes.toRequestBody(image.mimeType.toMediaType()),
    )

    private fun textPart(value: String) = value.toRequestBody(PLAIN_TEXT)

    private companion object {
        val PLAIN_TEXT = "text/plain".toMediaType()
    }
}
