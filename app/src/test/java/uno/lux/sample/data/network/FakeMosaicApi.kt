package uno.lux.sample.data.network

import uno.lux.sample.data.network.dto.AddCommentRequestDto
import uno.lux.sample.data.network.dto.AlbumDto
import uno.lux.sample.data.network.dto.BookmarkToggleDto
import uno.lux.sample.data.network.dto.CommentDto
import uno.lux.sample.data.network.dto.CursorPageDto
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.network.dto.LikeToggleDto
import uno.lux.sample.data.network.dto.PostFeedItemDto
import uno.lux.sample.data.network.dto.ProfileStatsDto
import uno.lux.sample.data.network.dto.MinimalUserDto
import uno.lux.sample.data.network.dto.UserDto
import uno.lux.sample.data.network.dto.VideoDto
import uno.lux.sample.data.network.response.BookmarkToggleResponse
import uno.lux.sample.data.network.response.CommentListResponse
import uno.lux.sample.data.network.response.CommentResponse
import uno.lux.sample.data.network.response.FeedResponse
import uno.lux.sample.data.network.response.LikeToggleResponse
import uno.lux.sample.data.network.response.ProfileStatsResponse
import uno.lux.sample.data.network.response.UserPostsResponse
import uno.lux.sample.data.network.response.UserResponse

internal val emptyPage = CursorPageDto(nextCursor = null, hasMore = false)

private val stubAuthor = userDto("u1", "Ada")

class FakeMosaicApi(
    private val feedResponse: FeedResponse = FeedResponse(data = emptyList(), page = emptyPage),
    /** Overrides [feedResponse] for specific cursor values, allowing multi-page feed tests. */
    private val feedResponseByCursor: Map<String?, FeedResponse> = emptyMap(),
    private val userById: Map<String, UserDto> = emptyMap(),
    private val profileStats: Map<String, ProfileStatsDto> = emptyMap(),
    private val userPosts: List<PostFeedItemDto> = emptyList(),
    private val comments: List<CommentDto> = emptyList(),
    val likeResult: LikeToggleDto = LikeToggleDto(isLiked = true, likeCount = 1),
    val bookmarkResult: BookmarkToggleDto = BookmarkToggleDto(isBookmarked = true),
) : MosaicApi {

    override suspend fun getFeed(cursor: String?, limit: Int, include: String): FeedResponse =
        feedResponseByCursor[cursor] ?: feedResponse

    override suspend fun getUser(id: String): UserResponse =
        UserResponse(userById[id] ?: error("FakeSampleApi: no user for id '$id'"))

    override suspend fun getProfileStats(id: String): ProfileStatsResponse =
        ProfileStatsResponse(profileStats[id] ?: ProfileStatsDto(0, 0, 0))

    override suspend fun getUserPosts(
        id: String,
        type: String?,
        cursor: String?,
        limit: Int,
    ): UserPostsResponse = UserPostsResponse(
        data = if (type == null) userPosts else userPosts.filter { it.matchesType(type) },
        page = emptyPage,
    )

    override suspend fun getComments(postId: String, cursor: String?, limit: Int): CommentListResponse =
        CommentListResponse(data = comments, page = emptyPage)

    override suspend fun toggleLike(postId: String, body: EmptyBody): LikeToggleResponse =
        LikeToggleResponse(likeResult)

    override suspend fun toggleBookmark(postId: String, body: EmptyBody): BookmarkToggleResponse =
        BookmarkToggleResponse(bookmarkResult)

    override suspend fun addComment(postId: String, body: AddCommentRequestDto): CommentResponse =
        CommentResponse(
            CommentDto(
                id = "c-new",
                text = body.text,
                createdAt = "2025-01-01T00:00:00.000Z",
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

private fun PostFeedItemDto.matchesType(type: String): Boolean = when (type) {
    "photo" -> album != null
    "video" -> video != null
    "text" -> album == null && video == null
    else -> true
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
    createdAt = "2025-01-01T00:00:00.000Z",
    authorId = authorId,
    likeCount = likeCount,
    commentCount = 0,
    isLiked = isLiked,
    isBookmarked = isBookmarked,
    album = album,
    video = video,
)

fun minimalUserDto(id: String, nickname: String) = MinimalUserDto(
    id = id,
    handle = "@$id",
    nickname = nickname,
)

fun userDto(id: String, nickname: String) = UserDto(
    id = id,
    nickname = nickname,
    handle = "@$id",
    age = null,
    gender = null,
    location = null,
    bio = null,
    followerCount = 0,
    followingCount = 0,
)

fun commentDto(
    id: String,
    text: String,
    isLiked: Boolean = false,
    likeCount: Int = 0,
) = CommentDto(
    id = id,
    text = text,
    createdAt = "2025-01-01T00:00:00.000Z",
    likeCount = likeCount,
    isLiked = isLiked,
    author = stubAuthor,
)
