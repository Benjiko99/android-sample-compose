package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.FollowToggleDto

@Serializable
data class FollowToggleResponse(val data: FollowToggleDto)
