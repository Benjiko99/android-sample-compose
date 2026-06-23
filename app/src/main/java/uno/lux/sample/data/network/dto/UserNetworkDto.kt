package uno.lux.sample.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserNetworkDto(
    val id: String,
    val nickname: String,
    val handle: String,
    val age: Int? = null,
    val gender: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val followerCount: Int,
    val followingCount: Int,
)
