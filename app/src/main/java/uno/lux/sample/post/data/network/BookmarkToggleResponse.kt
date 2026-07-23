package uno.lux.sample.post.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.post.data.network.BookmarkToggleDto

@Serializable
data class BookmarkToggleResponse(val data: BookmarkToggleDto)
