package uno.lux.sample.user.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.post.data.network.FollowToggleDto

@Serializable
data class FollowToggleResponse(val data: FollowToggleDto)
