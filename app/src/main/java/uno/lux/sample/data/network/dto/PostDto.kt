package uno.lux.sample.data.network.dto

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class PostFeedItemDto(
    val id: String,
    val title: String,
    val body: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val authorId: String,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val album: AlbumDto? = null,
    val video: VideoDto? = null,
)

/**
 * The server's full post projection — the same fields as [PostFeedItemDto] but with the author
 * embedded rather than referenced by ID. Returned by `POST /posts`.
 */
@Serializable
data class PostDto(
    val id: String,
    val title: String,
    val body: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val author: UserDto,
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
