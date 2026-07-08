package uno.lux.sample.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PostFeedItemDto(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val authorId: String,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val album: AlbumDto? = null,
    val video: VideoDto? = null,
)

@Serializable
data class AlbumDto(
    val id: String,
    val title: String,
    val itemCount: Int,
    val images: List<String>,
)

@Serializable
data class VideoDto(
    val id: String,
    val title: String,
    val durationSeconds: Int,
    val videoUrl: String,
)
