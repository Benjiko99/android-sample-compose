package uno.lux.sample.profile.data.network

import uno.lux.sample.app.core.network.emptyPage
import uno.lux.sample.user.data.network.SideloadedUsers
import uno.lux.sample.post.data.network.PostFeedItemDto

internal val emptyPostsWithAuthors = PostsWithAuthorsResponse(
    data = emptyList(),
    included = SideloadedUsers(users = emptyList()),
    page = emptyPage,
)

class FakeProfileApi(
    private val profileStats: Map<String, ProfileStatsDto> = emptyMap(),
    private val userPosts: List<PostFeedItemDto> = emptyList(),
    private val bookmarksResponse: PostsWithAuthorsResponse = emptyPostsWithAuthors,
    private val likesResponse: PostsWithAuthorsResponse = emptyPostsWithAuthors,
) : ProfileApi {

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
}
