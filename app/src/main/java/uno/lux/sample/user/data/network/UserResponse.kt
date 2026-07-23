package uno.lux.sample.user.data.network

import kotlinx.serialization.Serializable

/** `{ "data": T }` — single-resource response envelope. */
@Serializable
data class UserResponse(val data: UserDto)
