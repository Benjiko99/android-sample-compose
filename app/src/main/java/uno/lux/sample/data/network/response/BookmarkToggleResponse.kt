package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.BookmarkToggleDto

@Serializable
data class BookmarkToggleResponse(val data: BookmarkToggleDto)
