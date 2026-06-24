package uno.lux.sample.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileStatsDto(
    val postsCount: Int,
    val albumsCount: Int,
    val videosCount: Int,
)
