package uno.lux.sample.data

/**
 * An image album. On the profile grid the cover is a deterministic gradient stand-in keyed by
 * [id]; an album shown as a feed post instead renders its [images] (remote URLs) in a
 * horizontally scrolling row. [itemCount] is the album's full size, which can exceed the number
 * of [images] actually attached for preview.
 */
data class Album(
    val id: String,
    val title: String,
    val itemCount: Int,
    val images: List<String> = emptyList(),
)
