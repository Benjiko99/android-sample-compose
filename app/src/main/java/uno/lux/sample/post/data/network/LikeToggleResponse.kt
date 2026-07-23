package uno.lux.sample.post.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.post.data.network.LikeToggleDto

@Serializable
data class LikeToggleResponse(val data: LikeToggleDto)
