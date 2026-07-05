package uno.lux.sample.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class MinimalUserDto(
    val id: String,
    val handle: String,
    val nickname: String,
    val avatarUrl: String? = null,
)
