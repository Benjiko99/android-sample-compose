package uno.lux.sample.post.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.common.data.network.InstantSerializer
import uno.lux.sample.user.data.network.UserDto
import java.time.Instant

@Serializable
data class PostFeedItemDto(
    val id: String,
    val url: String,
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
    val url: String,
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

/**
 * Everything but the first four fields is nullable on the wire: the server measures the clip with
 * ffprobe and extracts the poster frame with ffmpeg, and any of that can fail on a file it cannot
 * read — in which case the video is still published, just without a resolution or a thumbnail.
 * Defaulting them also keeps the client readable against a server that predates the fields.
 */
@Serializable
data class VideoDto(
    val id: String,
    val title: String,
    val durationSeconds: Int,
    val videoUrl: String,
    val width: Int? = null,
    val height: Int? = null,
    val thumbnailUrl: String? = null,
    val thumbnailWidth: Int? = null,
    val thumbnailHeight: Int? = null,
)
