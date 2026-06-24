package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.LikeToggleDto

@Serializable
data class LikeToggleResponse(val data: LikeToggleDto)
