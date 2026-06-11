package uno.lux.sample.data

/**
 * An image album metadata. The cover itself is a deterministic gradient stand-in keyed
 * by [id], avoiding external image-loading dependencies.
 */
data class Album(
    val id: String,
    val title: String,
    val itemCount: Int,
)
