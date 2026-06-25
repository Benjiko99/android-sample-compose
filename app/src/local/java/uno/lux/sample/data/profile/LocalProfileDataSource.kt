package uno.lux.sample.data.profile

import uno.lux.sample.data.SampleAlbums
import uno.lux.sample.data.SamplePosts
import uno.lux.sample.data.SampleVideos
import uno.lux.sample.data.user.UserId

class LocalProfileDataSource : ProfileDataSource {

    override suspend fun refresh(userId: UserId): ProfileRefreshData {
        val posts = SamplePosts.filter { it.authorId == userId }
        val albums = SampleAlbums[userId].orEmpty()
        val videos = SampleVideos[userId].orEmpty()

        return ProfileRefreshData(
            postsCount = posts.size,
            albumsCount = albums.size,
            videosCount = videos.size,
            posts = posts,
            postCursor = null,
            postHasMore = false,
            albums = albums,
            albumCursor = null,
            albumHasMore = false,
            videos = videos,
            videoCursor = null,
            videoHasMore = false,
        )
    }

    override suspend fun loadMorePosts(userId: UserId, cursor: String?) =
        PostsPage(emptyList(), null, false)

    override suspend fun loadMoreAlbums(userId: UserId, cursor: String?) =
        AlbumsPage(emptyList(), null, false)

    override suspend fun loadMoreVideos(userId: UserId, cursor: String?) =
        VideosPage(emptyList(), null, false)
}
