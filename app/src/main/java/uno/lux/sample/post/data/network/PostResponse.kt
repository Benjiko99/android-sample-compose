package uno.lux.sample.post.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.post.data.network.PostDto

@Serializable
data class PostResponse(val data: PostDto)
