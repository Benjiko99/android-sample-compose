package uno.lux.sample.data.profile

import uno.lux.sample.data.post.Album
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.user.UserId

interface ProfileDataSource {
    suspend fun refresh(userId: UserId): ProfileRefreshData
    suspend fun loadMorePosts(userId: UserId, cursor: String?): PostsPage
    suspend fun loadMoreAlbums(userId: UserId, cursor: String?): AlbumsPage
    suspend fun loadMoreVideos(userId: UserId, cursor: String?): VideosPage
}

data class ProfileRefreshData(
    val postsCount: Int,
    val albumsCount: Int,
    val videosCount: Int,
    val posts: List<Post>,
    val postCursor: String?,
    val postHasMore: Boolean,
    val albums: List<Album>,
    val albumCursor: String?,
    val albumHasMore: Boolean,
    val videos: List<Video>,
    val videoCursor: String?,
    val videoHasMore: Boolean,
)

data class PostsPage(val posts: List<Post>, val cursor: String?, val hasMore: Boolean)
data class AlbumsPage(val albums: List<Album>, val cursor: String?, val hasMore: Boolean)
data class VideosPage(val videos: List<Video>, val cursor: String?, val hasMore: Boolean)
