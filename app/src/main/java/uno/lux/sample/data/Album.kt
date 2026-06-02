package uno.lux.sample.data

/**
 * An image album shown as a cover thumbnail in the profile's Albums grid. [itemCount] drives
 * the stack/count badge; the cover itself is a deterministic gradient stand-in keyed by [id],
 * so there is no image-loading dependency.
 */
data class Album(
    val id: String,
    val title: String,
    val itemCount: Int,
)
