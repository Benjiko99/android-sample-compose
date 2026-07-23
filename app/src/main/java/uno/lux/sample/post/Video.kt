package uno.lux.sample.post

typealias VideoId = String

data class Video(
    val id: VideoId,
    val title: String,
    val durationSeconds: Int,
    val videoUrl: String,
)
