package uno.lux.sample.data.post

typealias AlbumId = String

/**
 * An image album attached to a post, rendered as a horizontally scrolling row of its [images]
 * (remote URLs). [itemCount] is the album's full size, which can exceed the number of [images]
 * actually attached for preview.
 */
data class Album(
    val id: AlbumId,
    val title: String,
    val itemCount: Int,
    val images: List<String> = emptyList(),
)
