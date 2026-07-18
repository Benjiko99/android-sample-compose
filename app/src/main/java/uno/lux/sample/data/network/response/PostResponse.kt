package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.PostDto

@Serializable
data class PostResponse(val data: PostDto)
