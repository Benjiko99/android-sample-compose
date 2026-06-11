package uno.lux.sample.data

/**
 * An aggregate of a [user]'s content.
 */
data class Profile(
    val user: User,
    val posts: List<Post>,
    val albums: List<Album>,
    val videos: List<Video>,
) {
    val postCount: Int get() = posts.size
    val albumCount: Int get() = albums.size
    val videoCount: Int get() = videos.size
}
