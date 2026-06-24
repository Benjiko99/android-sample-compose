package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.CommentNetworkDto

@Serializable
data class CommentResponse(val data: CommentNetworkDto)
