package uno.lux.sample.data.network

import java.time.Instant
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.Buffer
import uno.lux.sample.data.network.dto.AddCommentRequestDto
import uno.lux.sample.data.network.dto.AlbumDto
import uno.lux.sample.data.network.dto.BookmarkToggleDto
import uno.lux.sample.data.network.dto.CommentDto
import uno.lux.sample.data.network.dto.CursorPageDto
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.network.dto.FollowToggleDto
import uno.lux.sample.data.network.dto.LikeToggleDto
import uno.lux.sample.data.network.dto.PostDto
import uno.lux.sample.data.network.dto.PostFeedItemDto
import uno.lux.sample.data.network.dto.ProfileStatsDto
import uno.lux.sample.data.network.dto.UserDto
import uno.lux.sample.data.network.dto.VideoDto
import uno.lux.sample.data.network.response.BookmarkToggleResponse
import uno.lux.sample.data.network.response.PostsWithAuthorsResponse
import uno.lux.sample.data.network.response.CommentListResponse
import uno.lux.sample.data.network.response.CommentResponse
import uno.lux.sample.data.network.response.FeedIncluded
import uno.lux.sample.data.network.response.FeedResponse
import uno.lux.sample.data.network.response.FollowToggleResponse
import uno.lux.sample.data.network.response.LikeToggleResponse
import uno.lux.sample.data.network.response.PostResponse
import uno.lux.sample.data.network.response.ProfileStatsResponse
import uno.lux.sample.data.network.response.UserPostsResponse
import uno.lux.sample.data.network.response.UserResponse

internal val emptyPage = CursorPageDto(nextCursor = null, hasMore = false)

internal val emptyPostsWithAuthors = PostsWithAuthorsResponse(
    data = emptyList(),
    included = FeedIncluded(users = emptyList()),
    page = emptyPage,
)

/** The hosted URL the fake reports after an avatar file is uploaded. */
const val UPLOADED_AVATAR_URL = "https://cdn.test/avatars/uploaded.png"

private val stubAuthor = userDto("u1", "Ada")

class FakeMosaicApi(
    private val feedResponse: FeedResponse = FeedResponse(data = emptyList(), page = emptyPage),
    /** Overrides [feedResponse] for specific cursor values, allowing multi-page feed tests. */
    private val feedResponseByCursor: Map<String?, FeedResponse> = emptyMap(),
    private val userById: Map<String, UserDto> = emptyMap(),
    private val profileStats: Map<String, ProfileStatsDto> = emptyMap(),
    private val userPosts: List<PostFeedItemDto> = emptyList(),
    private val bookmarksResponse: PostsWithAuthorsResponse = emptyPostsWithAuthors,
    private val likesResponse: PostsWithAuthorsResponse = emptyPostsWithAuthors,
    private val comments: List<CommentDto> = emptyList(),
    val likeResult: LikeToggleDto = LikeToggleDto(isLiked = true, likeCount = 1),
    val bookmarkResult: BookmarkToggleDto = BookmarkToggleDto(isBookmarked = true),
    val followResult: FollowToggleDto = FollowToggleDto(isFollowing = true, followerCount = 1),
) : MosaicApi {

    /** The id passed to the most recent [toggleFollow] call, for test assertions. */
    var lastFollowId: String? = null
        private set

    override suspend fun getFeed(cursor: String?, limit: Int, include: String): FeedResponse =
        feedResponseByCursor[cursor] ?: feedResponse

    override suspend fun getUser(id: String): UserResponse =
        UserResponse(userById[id] ?: error("FakeSampleApi: no user for id '$id'"))

    override suspend fun updateUser(
        id: String,
        nickname: RequestBody,
        age: RequestBody,
        gender: RequestBody,
        bio: RequestBody,
        avatar: MultipartBody.Part?,
    ): UserResponse {
        val base = userById[id] ?: error("FakeSampleApi: no user for id '$id'")
        lastAvatarPart = avatar

        // Mirror the server: empty text parts clear the nullable fields; a present avatar
        // file yields a fresh hosted URL, its absence leaves the current avatar in place.
        return UserResponse(
            base.copy(
                nickname = nickname.asString(),
                age = age.asString().toIntOrNull(),
                gender = gender.asString().ifEmpty { null },
                bio = bio.asString().ifEmpty { null },
                avatarUrl = if (avatar != null) UPLOADED_AVATAR_URL else base.avatarUrl,
            )
        )
    }

    /** The avatar part passed to the most recent [updateUser] call, for test assertions. */
    var lastAvatarPart: MultipartBody.Part? = null
        private set

    private fun RequestBody.asString(): String = Buffer().also { writeTo(it) }.readUtf8()

    override suspend fun toggleFollow(id: String): FollowToggleResponse {
        lastFollowId = id
        return FollowToggleResponse(followResult)
    }

    override suspend fun getProfileStats(id: String): ProfileStatsResponse =
        ProfileStatsResponse(profileStats[id] ?: ProfileStatsDto(postsCount = 0))

    override suspend fun getUserPosts(
        id: String,
        cursor: String?,
        limit: Int,
    ): UserPostsResponse = UserPostsResponse(data = userPosts, page = emptyPage)

    /** The (id, cursor) pairs [getBookmarks] was called with, in call order. */
    val bookmarkCalls = mutableListOf<Pair<String, String?>>()

    override suspend fun getBookmarks(
        id: String,
        cursor: String?,
        limit: Int,
    ): PostsWithAuthorsResponse {
        bookmarkCalls += id to cursor
        return bookmarksResponse
    }

    /** The (id, cursor) pairs [getLikes] was called with, in call order. */
    val likeCalls = mutableListOf<Pair<String, String?>>()

    override suspend fun getLikes(
        id: String,
        cursor: String?,
        limit: Int,
    ): PostsWithAuthorsResponse {
        likeCalls += id to cursor
        return likesResponse
    }

    override suspend fun getComments(postId: String, cursor: String?, limit: Int): CommentListResponse =
        CommentListResponse(data = comments, page = emptyPage)

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

        return PostResponse(
            PostDto(
                id = "p-new",
                title = titleText,
                body = bodyText,
                createdAt = Instant.parse("2025-01-01T00:00:00.000Z"),
                author = stubAuthor,
                likeCount = 0,
                commentCount = 0,
                isLiked = false,
                isBookmarked = false,
            )
        )
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

    override suspend fun addComment(postId: String, body: AddCommentRequestDto): CommentResponse =
        CommentResponse(
            CommentDto(
                id = "c-new",
                text = body.text,
                createdAt = Instant.parse("2025-01-01T00:00:00.000Z"),
                likeCount = 0,
                isLiked = false,
                author = stubAuthor,
            )
        )

    override suspend fun toggleCommentLike(
        postId: String,
        commentId: String,
        body: EmptyBody,
    ): LikeToggleResponse = LikeToggleResponse(likeResult)
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

fun userDto(id: String, nickname: String, isFollowing: Boolean = false) = UserDto(
    id = id,
    nickname = nickname,
    handle = "@$id",
    age = null,
    gender = null,
    location = null,
    bio = null,
    followerCount = 0,
    followingCount = 0,
    isFollowing = isFollowing,
)

fun commentDto(
    id: String,
    text: String,
    isLiked: Boolean = false,
    likeCount: Int = 0,
) = CommentDto(
    id = id,
    text = text,
    createdAt = Instant.parse("2025-01-01T00:00:00.000Z"),
    likeCount = likeCount,
    isLiked = isLiked,
    author = stubAuthor,
)
