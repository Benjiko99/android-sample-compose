package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.UserDto

/** `{ "data": T }` — single-resource response envelope. */
@Serializable
data class UserResponse(val data: UserDto)
