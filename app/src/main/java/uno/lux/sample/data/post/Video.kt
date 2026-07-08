package uno.lux.sample.data.post

typealias VideoId = String

data class Video(
    val id: VideoId,
    val title: String,
    val durationSeconds: Int,
    /** The streamable source the player opens; see [SampleVideoUrl] for the stand-in. */
    val videoUrl: String,
)
