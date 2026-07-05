package uno.lux.sample.data.user

/**
 * The editable slice of a [User]'s profile, applied atomically by
 * [UserRepository.updateProfile]. `null` clears an optional field; [gender] carries one of
 * the canonical values the backend accepts ("Man" / "Woman").
 */
data class ProfileUpdate(
    val nickname: String,
    val age: Int?,
    val gender: String?,
    val bio: String?,
    val avatarUrl: String?,
)
