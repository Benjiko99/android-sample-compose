package uno.lux.sample.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Body of `PATCH /users/{id}`. The client always sends the full editable set; explicit
 * `null`s clear the optional fields server-side.
 */
@Serializable
data class UpdateUserRequestDto(
    val nickname: String,
    val age: Int?,
    val gender: String?,
    val bio: String?,
    val avatarUrl: String?,
)
