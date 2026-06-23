package uno.lux.sample.data.network.dto

import kotlinx.serialization.Serializable

/** `{ "data": T }` — single-resource response envelope. */
@Serializable
data class UserResponse(val data: UserNetworkDto)

@Serializable
data class ProfileStatsResponse(val data: ProfileStatsNetworkDto)

@Serializable
data class LikeToggleResponse(val data: LikeToggleNetworkDto)

@Serializable
data class BookmarkToggleResponse(val data: BookmarkToggleNetworkDto)

@Serializable
data class CommentResponse(val data: CommentNetworkDto)

/** Top-level feed response — not wrapped in an extra `data` key. */
@Serializable
data class FeedResponse(
    val data: List<PostFeedItemNetworkDto>,
    val included: FeedIncluded? = null,
    val page: CursorPageDto,
)

@Serializable
data class FeedIncluded(val users: List<UserNetworkDto>)

/** `{ "data": [...], "page": { ... } }` — cursor-paginated list envelope. */
@Serializable
data class UserPostsResponse(
    val data: List<PostFeedItemNetworkDto>,
    val page: CursorPageDto,
)

@Serializable
data class CommentListResponse(
    val data: List<CommentNetworkDto>,
    val page: CursorPageDto,
)
