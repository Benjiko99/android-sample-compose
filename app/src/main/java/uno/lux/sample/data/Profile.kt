package uno.lux.sample.data

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
