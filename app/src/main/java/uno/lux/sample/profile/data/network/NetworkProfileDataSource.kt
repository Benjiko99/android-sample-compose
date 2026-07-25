package uno.lux.sample.profile.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import uno.lux.sample.post.data.network.PostMapper
import uno.lux.sample.profile.data.PostsPage
import uno.lux.sample.profile.data.PostsWithAuthorsPage
import uno.lux.sample.profile.data.ProfileDataSource
import uno.lux.sample.profile.data.ProfileRefreshData
import uno.lux.sample.user.data.domain.UserId
import uno.lux.sample.user.data.network.toDomain

class NetworkProfileDataSource(
    private val api: ProfileApi,
) : ProfileDataSource {

    override suspend fun refresh(userId: UserId): ProfileRefreshData = withContext(Dispatchers.IO) {
        val statsDeferred = async { api.getProfileStats(userId).data }
        val postsDeferred = async { api.getUserPosts(userId) }

        val stats = statsDeferred.await()
        val postsResponse = postsDeferred.await()

        ProfileRefreshData(
            postsCount = stats.postsCount,
            posts = postsResponse.data.map { PostMapper.map(it) },
            postCursor = postsResponse.page.nextCursor,
            postHasMore = postsResponse.page.hasMore,
        )
    }

    override suspend fun bookmarks(userId: UserId, cursor: String?): PostsWithAuthorsPage =
        withContext(Dispatchers.IO) { api.getBookmarks(userId, cursor = cursor).toPage() }

    override suspend fun likes(userId: UserId, cursor: String?): PostsWithAuthorsPage =
        withContext(Dispatchers.IO) { api.getLikes(userId, cursor = cursor).toPage() }

    private fun PostsWithAuthorsResponse.toPage() = PostsWithAuthorsPage(
        posts = data.map { PostMapper.map(it) },
        users = included.users.map { it.toDomain() },
        cursor = page.nextCursor,
        hasMore = page.hasMore,
    )

    override suspend fun loadMorePosts(userId: UserId, cursor: String?): PostsPage = withContext(Dispatchers.IO) {
        val response = api.getUserPosts(userId, cursor = cursor)
        PostsPage(
            posts = response.data.map { PostMapper.map(it) },
            cursor = response.page.nextCursor,
            hasMore = response.page.hasMore,
        )
    }
}
