package uno.lux.sample.common.data.network

import kotlinx.serialization.Serializable

@Serializable
data class LikeToggleResponse(
    val data: LikeToggleDto,
)
