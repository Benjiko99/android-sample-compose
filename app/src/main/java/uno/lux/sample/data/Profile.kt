package uno.lux.sample.data

/**
 * Everything the profile screen renders for one [user]: their text [posts], image [albums]
 * and [videos]. The per-tab counts are derived from the lists so they can never drift from
 * the content actually shown.
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
