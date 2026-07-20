package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import uno.lux.sample.data.profile.BookmarksPage
import uno.lux.sample.data.profile.PostsPage
import uno.lux.sample.data.profile.ProfileDataSource
import uno.lux.sample.data.profile.ProfileRefreshData
import uno.lux.sample.data.user.UserId

class NetworkProfileDataSource(
    private val api: MosaicApi,
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

    override suspend fun bookmarks(userId: UserId, cursor: String?): BookmarksPage = withContext(Dispatchers.IO) {
        val response = api.getBookmarks(userId, cursor = cursor)
        BookmarksPage(
            posts = response.data.map { PostMapper.map(it) },
            users = response.included.users.map { it.toDomain() },
            cursor = response.page.nextCursor,
            hasMore = response.page.hasMore,
        )
    }

    override suspend fun loadMorePosts(userId: UserId, cursor: String?): PostsPage = withContext(Dispatchers.IO) {
        val response = api.getUserPosts(userId, cursor = cursor)
        PostsPage(
            posts = response.data.map { PostMapper.map(it) },
            cursor = response.page.nextCursor,
            hasMore = response.page.hasMore,
        )
    }
}
