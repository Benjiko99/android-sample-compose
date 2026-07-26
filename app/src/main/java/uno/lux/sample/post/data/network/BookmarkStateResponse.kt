package uno.lux.sample.post.data.network

import kotlinx.serialization.Serializable

@Serializable
data class BookmarkStateResponse(
    val data: BookmarkStateDto,
)
