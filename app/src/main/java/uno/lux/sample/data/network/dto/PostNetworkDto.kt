package uno.lux.sample.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PostFeedItemNetworkDto(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val authorId: String,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val album: AlbumNetworkDto? = null,
    val video: VideoNetworkDto? = null,
)

@Serializable
data class AlbumNetworkDto(
    val id: String,
    val title: String,
    val itemCount: Int,
    val images: List<String>,
)

@Serializable
data class VideoNetworkDto(
    val id: String,
    val title: String,
    val durationSeconds: Int,
    val viewCount: Int,
    val videoUrl: String,
)
