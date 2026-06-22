package uno.lux.sample.data.profile

import uno.lux.sample.data.User
import uno.lux.sample.data.post.Album
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.Video

/**
 * An aggregate of a [user]'s content.
 */
data class Profile(
    val user: User,
    val posts: List<Post>,
    val albums: List<Album>,
    val videos: List<Video>,
    val postsCount: Int,
    val albumsCount: Int,
    val videosCount: Int,
)
