package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.BookmarkToggleNetworkDto

@Serializable
data class BookmarkToggleResponse(val data: BookmarkToggleNetworkDto)
