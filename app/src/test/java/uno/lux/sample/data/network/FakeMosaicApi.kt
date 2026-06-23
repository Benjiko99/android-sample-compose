package uno.lux.sample.data.network

import uno.lux.sample.data.network.dto.AddCommentRequestDto
import uno.lux.sample.data.network.dto.AlbumNetworkDto
import uno.lux.sample.data.network.dto.BookmarkToggleNetworkDto
import uno.lux.sample.data.network.dto.BookmarkToggleResponse
import uno.lux.sample.data.network.dto.CommentListResponse
import uno.lux.sample.data.network.dto.CommentNetworkDto
import uno.lux.sample.data.network.dto.CommentResponse
import uno.lux.sample.data.network.dto.CursorPageDto
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.network.dto.FeedResponse
import uno.lux.sample.data.network.dto.LikeToggleNetworkDto
import uno.lux.sample.data.network.dto.LikeToggleResponse
import uno.lux.sample.data.network.dto.PostFeedItemNetworkDto
import uno.lux.sample.data.network.dto.ProfileStatsNetworkDto
import uno.lux.sample.data.network.dto.ProfileStatsResponse
import uno.lux.sample.data.network.dto.UserNetworkDto
import uno.lux.sample.data.network.dto.UserPostsResponse
import uno.lux.sample.data.network.dto.UserResponse
import uno.lux.sample.data.network.dto.VideoNetworkDto

internal val emptyPage = CursorPageDto(nextCursor = null, hasMore = false)

private val stubAuthor = userDto("u1", "Ada")

class FakeMosaicApi(
    private val feedResponse: FeedResponse = FeedResponse(data = emptyList(), page = emptyPage),
    private val userById: Map<String, UserNetworkDto> = emptyMap(),
    private val profileStats: Map<String, ProfileStatsNetworkDto> = emptyMap(),
    private val userPosts: List<PostFeedItemNetworkDto> = emptyList(),
    private val comments: List<CommentNetworkDto> = emptyList(),
    val likeResult: LikeToggleNetworkDto = LikeToggleNetworkDto(isLiked = true, likeCount = 1),
    val bookmarkResult: BookmarkToggleNetworkDto = BookmarkToggleNetworkDto(isBookmarked = true),
) : MosaicApi {

    override suspend fun getFeed(cursor: String?, limit: Int, include: String): FeedResponse =
        feedResponse

    override suspend fun getUser(id: String): UserResponse =
        UserResponse(userById[id] ?: error("FakeSampleApi: no user for id '$id'"))

    override suspend fun getProfileStats(id: String): ProfileStatsResponse =
        ProfileStatsResponse(profileStats[id] ?: ProfileStatsNetworkDto(0, 0, 0))

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
            CommentNetworkDto(
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

private fun PostFeedItemNetworkDto.matchesType(type: String): Boolean = when (type) {
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
    album: AlbumNetworkDto? = null,
    video: VideoNetworkDto? = null,
) = PostFeedItemNetworkDto(
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

fun userDto(id: String, nickname: String) = UserNetworkDto(
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
) = CommentNetworkDto(
    id = id,
    text = text,
    createdAt = "2025-01-01T00:00:00.000Z",
    likeCount = likeCount,
    isLiked = isLiked,
    author = stubAuthor,
)
