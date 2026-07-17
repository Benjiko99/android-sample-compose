package uno.lux.sample.data.post

typealias VideoId = String

data class Video(
    val id: VideoId,
    val title: String,
    val durationSeconds: Int,
    val videoUrl: String,
)
