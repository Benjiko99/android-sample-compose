package uno.lux.sample.post

typealias AlbumId = String

data class Album(
    val id: AlbumId,
    val title: String,
    val itemCount: Int,
    val images: List<String> = emptyList(),
)
