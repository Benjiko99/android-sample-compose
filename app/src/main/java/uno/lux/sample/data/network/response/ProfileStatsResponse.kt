package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.ProfileStatsDto

@Serializable
data class ProfileStatsResponse(val data: ProfileStatsDto)
