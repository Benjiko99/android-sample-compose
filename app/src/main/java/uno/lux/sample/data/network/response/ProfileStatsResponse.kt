package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.ProfileStatsNetworkDto

@Serializable
data class ProfileStatsResponse(val data: ProfileStatsNetworkDto)
