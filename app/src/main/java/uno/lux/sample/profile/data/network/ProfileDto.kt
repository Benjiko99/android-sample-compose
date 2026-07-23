package uno.lux.sample.profile.data.network

import kotlinx.serialization.Serializable

@Serializable
data class ProfileStatsDto(
    val postsCount: Int,
)
