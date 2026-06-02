package uno.lux.sample.data

/**
 * A video shown as a thumbnail in the profile's Videos grid. [durationSeconds] renders as the
 * duration badge and [viewCount] as the view caption; the thumbnail is a deterministic gradient
 * stand-in keyed by [id], so there is no image-loading dependency.
 */
data class Video(
    val id: String,
    val title: String,
    val durationSeconds: Int,
    val viewCount: Int,
)
