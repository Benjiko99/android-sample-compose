package uno.lux.sample.data.post

data class Video(
    val id: String,
    val title: String,
    val durationSeconds: Int,
    val viewCount: Int,
    /** The streamable source the player opens; see [SampleVideoUrl] for the stand-in. */
    val videoUrl: String,
)
