package uno.lux.sample.profile.data.network

import kotlinx.serialization.Serializable

@Serializable
data class ProfileStatsResponse(
    val data: ProfileStatsDto,
)
