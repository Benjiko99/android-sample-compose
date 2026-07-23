package uno.lux.sample.profile.data.network

import kotlinx.serialization.Serializable
import uno.lux.sample.profile.data.network.ProfileStatsDto

@Serializable
data class ProfileStatsResponse(val data: ProfileStatsDto)
