package uno.lux.sample.post.data.network

import java.time.Instant
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.Buffer
import uno.lux.sample.app.core.network.EmptyBody
import uno.lux.sample.app.common.data.network.LikeToggleDto
import uno.lux.sample.app.common.data.network.LikeToggleResponse
import uno.lux.sample.app.core.network.notFoundException
import uno.lux.sample.testing.testPostUrl
import uno.lux.sample.user.data.network.UserDto
import uno.lux.sample.user.data.network.stubAuthor

class FakePostApi(
    /** What [getPost] serves; an id that isn't here 404s, the way the real API does. */
    private val postById: Map<String, PostDto> = emptyMap(),
    val likeResult: LikeToggleDto = LikeToggleDto(isLiked = true, likeCount = 1),
    val bookmarkResult: BookmarkToggleDto = BookmarkToggleDto(isBookmarked = true),
) : PostApi {

    /** IDs passed to [getPost], in call order, for test assertions. */
    val fetchedPostIds = mutableListOf<String>()

    /** Thrown by [getPost] instead of answering, so tests can drive the failure path. */
    var getPostError: Exception? = null

    override suspend fun getPost(postId: String): PostResponse {
        fetchedPostIds += postId
        getPostError?.let { throw it }

        val dto = postById[postId] ?: throw notFoundException("Post '$postId' was not found")
        return PostResponse(dto)
    }

    /** The multipart parts of the most recent [createPost] call, for test assertions. */
    var lastCreatePostParts: CreatePostParts? = null
        private set

    /** The parts [createPost] received, decoded back to the values a test can compare against. */
    data class CreatePostParts(
        val title: String,
        val body: String,
        val images: List<UploadedPart>,
        val video: UploadedPart? = null,
        val videoDurationSeconds: String? = null,
    )

    /** One `images[]` part: the filename it declared and the bytes it carried. */
    data class UploadedPart(val filename: String?, val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is UploadedPart) return false

            return filename == other.filename && bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int = 31 * (filename?.hashCode() ?: 0) + bytes.contentHashCode()
    }

    override suspend fun createPost(
        title: RequestBody,
        body: RequestBody,
        images: List<MultipartBody.Part>,
        video: MultipartBody.Part?,
        videoDurationSeconds: RequestBody?,
    ): PostResponse {
        val titleText = title.readText()
        val bodyText = body.readText()
        lastCreatePostParts = CreatePostParts(
            title = titleText,
            body = bodyText,
            images = images.map { part ->
                UploadedPart(filename = part.filename(), bytes = part.body.readBytes())
            },
            video = video?.let {
                UploadedPart(filename = it.filename(), bytes = it.body.readBytes())
            },
            videoDurationSeconds = videoDurationSeconds?.readText(),
        )

        return PostResponse(fullPostDto("p-new").copy(title = titleText, body = bodyText))
    }

    // Retrofit hands the fake real RequestBody parts, so the assertions read them back out.
    private fun RequestBody.readText() = Buffer().also { writeTo(it) }.readUtf8()

    private fun RequestBody.readBytes() = Buffer().also { writeTo(it) }.readByteArray()

    /** The `filename="…"` of a part's Content-Disposition header, which is how the server names it. */
    private fun MultipartBody.Part.filename(): String? =
        headers?.get("Content-Disposition")
            ?.substringAfter("filename=\"", missingDelimiterValue = "")
            ?.substringBefore('"')
            ?.takeIf { it.isNotEmpty() }

    /** IDs passed to [deletePost], in call order, for test assertions. */
    val deletedPostIds = mutableListOf<String>()

    override suspend fun deletePost(postId: String) {
        deletedPostIds += postId
    }

    override suspend fun toggleLike(postId: String, body: EmptyBody): LikeToggleResponse =
        LikeToggleResponse(likeResult)

    override suspend fun toggleBookmark(postId: String, body: EmptyBody): BookmarkToggleResponse =
        BookmarkToggleResponse(bookmarkResult)
}

fun feedItemDto(
    id: String,
    authorId: String,
    isLiked: Boolean = false,
    isBookmarked: Boolean = false,
    likeCount: Int = 0,
    album: AlbumDto? = null,
    video: VideoDto? = null,
) = PostFeedItemDto(
    id = id,
    url = testPostUrl(id),
    title = "Title $id",
    body = "Body $id",
    createdAt = Instant.parse("2025-01-01T00:00:00.000Z"),
    authorId = authorId,
    likeCount = likeCount,
    commentCount = 0,
    isLiked = isLiked,
    isBookmarked = isBookmarked,
    album = album,
    video = video,
)

/** The full projection — [feedItemDto]'s counterpart with the author embedded. */
fun fullPostDto(
    id: String,
    author: UserDto = stubAuthor,
    isLiked: Boolean = false,
    isBookmarked: Boolean = false,
    likeCount: Int = 0,
    album: AlbumDto? = null,
    video: VideoDto? = null,
) = PostDto(
    id = id,
    url = testPostUrl(id),
    title = "Title $id",
    body = "Body $id",
    createdAt = Instant.parse("2025-01-01T00:00:00.000Z"),
    author = author,
    likeCount = likeCount,
    commentCount = 0,
    isLiked = isLiked,
    isBookmarked = isBookmarked,
    album = album,
    video = video,
)
