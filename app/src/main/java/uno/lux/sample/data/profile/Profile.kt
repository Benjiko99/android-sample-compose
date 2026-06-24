package uno.lux.sample.data.profile

import uno.lux.sample.data.post.Album
import uno.lux.sample.data.post.Video

/** A user's profile metadata: counts, albums, and videos. Posts are owned by the screen layer. */
data class Profile(
    val userId: String,
    val albums: List<Album>,
    val videos: List<Video>,
    val postsCount: Int,
    val albumsCount: Int,
    val videosCount: Int,
)
