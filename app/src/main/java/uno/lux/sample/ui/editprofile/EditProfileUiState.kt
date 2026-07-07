package uno.lux.sample.ui.editprofile

import androidx.annotation.StringRes
import uno.lux.sample.R
import uno.lux.sample.data.user.AvatarUpload
import uno.lux.sample.data.user.ProfileUpdate
import uno.lux.sample.data.user.User
import uno.lux.sample.util.AppError

/**
 * The gender choices the profile editor offers. [storedValue] is the canonical value the
 * backend accepts and stores; [labelRes] is what the user sees, so the two can diverge under
 * localization without changing the data contract.
 */
enum class GenderOption(
    val storedValue: String,
    @get:StringRes val labelRes: Int,
) {
    MAN("Man", R.string.gender_man),
    WOMAN("Woman", R.string.gender_woman);

    companion object {
        fun fromStored(value: String?): GenderOption? =
            entries.firstOrNull { it.storedValue == value }
    }
}

/** Ages the editor accepts; mirrors the backend's validation so errors surface before a save. */
val EditProfileAgeRange = 13..120

/**
 * The editable form fields, seeded once from the loaded [User]. [age] stays the raw digit
 * string the user typed; it only becomes an [Int] in [toProfileUpdate]. [avatarUrl] is the
 * current server avatar; [pickedAvatarUri] is a newly chosen local image (a `content://` URI)
 * not yet uploaded — [displayAvatar] prefers it for the live preview.
 */
data class EditProfileForm(
    val nickname: String,
    val age: String,
    val gender: GenderOption?,
    val bio: String,
    val avatarUrl: String?,
    val pickedAvatarUri: String? = null,
) {
    val displayAvatar: String?
        get() = pickedAvatarUri ?: avatarUrl

    val isAgeValid: Boolean
        get() {
            if (age.isEmpty()) return true
            val value = age.toIntOrNull() ?: return false

            return value in EditProfileAgeRange
        }

    val canSave: Boolean
        get() = nickname.isNotBlank() && isAgeValid

    /** [avatar] is the uploaded bytes of [pickedAvatarUri], resolved by the ViewModel on save. */
    fun toProfileUpdate(avatar: AvatarUpload?) = ProfileUpdate(
        nickname = nickname.trim(),
        age = age.toIntOrNull(),
        gender = gender?.storedValue,
        bio = bio.trim().ifEmpty { null },
        avatar = avatar,
    )

    companion object {
        fun from(user: User) = EditProfileForm(
            nickname = user.nickname,
            age = user.age?.toString().orEmpty(),
            gender = GenderOption.fromStored(user.gender),
            bio = user.bio.orEmpty(),
            avatarUrl = user.avatarUrl,
        )
    }
}

/** The edit-profile screen's state: loading the profile, a failed load, or the live form. */
sealed interface EditProfileUiState {
    data object Loading : EditProfileUiState
    data class Error(val error: AppError) : EditProfileUiState

    data class Editing(
        val form: EditProfileForm,
        val isSaving: Boolean,
        val saveError: AppError?,
    ) : EditProfileUiState
}
