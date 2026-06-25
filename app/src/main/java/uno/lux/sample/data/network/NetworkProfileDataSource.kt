package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import uno.lux.sample.data.profile.AlbumsPage
import uno.lux.sample.data.profile.PostsPage
import uno.lux.sample.data.profile.ProfileDataSource
import uno.lux.sample.data.profile.ProfileRefreshData
import uno.lux.sample.data.profile.VideosPage
import uno.lux.sample.data.user.UserId

class NetworkProfileDataSource(
    private val api: MosaicApi,
) : ProfileDataSource {

    override suspend fun refresh(userId: UserId): ProfileRefreshData = withContext(Dispatchers.IO) {
        val statsDeferred = async { api.getProfileStats(userId).data }
        val postsDeferred = async { api.getUserPosts(userId) }
        val albumsDeferred = async { api.getUserPosts(userId, type = "photo") }
        val videosDeferred = async { api.getUserPosts(userId, type = "video") }

        val stats = statsDeferred.await()
        val postsResponse = postsDeferred.await()
        val albumsResponse = albumsDeferred.await()
        val videosResponse = videosDeferred.await()

        ProfileRefreshData(
            postsCount = stats.postsCount,
            albumsCount = stats.albumsCount,
            videosCount = stats.videosCount,
            posts = postsResponse.data.map { it.toDomain() },
            postCursor = postsResponse.page.nextCursor,
            postHasMore = postsResponse.page.hasMore,
            albums = albumsResponse.data.mapNotNull { it.album?.toDomain() },
            albumCursor = albumsResponse.page.nextCursor,
            albumHasMore = albumsResponse.page.hasMore,
            videos = videosResponse.data.mapNotNull { it.video?.toDomain() },
            videoCursor = videosResponse.page.nextCursor,
            videoHasMore = videosResponse.page.hasMore,
        )
    }

    override suspend fun loadMorePosts(userId: UserId, cursor: String?): PostsPage = withContext(Dispatchers.IO) {
        val response = api.getUserPosts(userId, cursor = cursor)
        PostsPage(
            posts = response.data.map { it.toDomain() },
            cursor = response.page.nextCursor,
            hasMore = response.page.hasMore,
        )
    }

    override suspend fun loadMoreAlbums(userId: UserId, cursor: String?): AlbumsPage = withContext(Dispatchers.IO) {
        val response = api.getUserPosts(userId, type = "photo", cursor = cursor)
        AlbumsPage(
            albums = response.data.mapNotNull { it.album?.toDomain() },
            cursor = response.page.nextCursor,
            hasMore = response.page.hasMore,
        )
    }

    override suspend fun loadMoreVideos(userId: UserId, cursor: String?): VideosPage = withContext(Dispatchers.IO) {
        val response = api.getUserPosts(userId, type = "video", cursor = cursor)
        VideosPage(
            videos = response.data.mapNotNull { it.video?.toDomain() },
            cursor = response.page.nextCursor,
            hasMore = response.page.hasMore,
        )
    }
}
