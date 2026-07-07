package uno.lux.sample.data.user

/**
 * The editable slice of a [User]'s profile, applied atomically by
 * [UserRepository.updateProfile]. `null` clears an optional text field; [gender] carries one
 * of the canonical values the backend accepts ("Man" / "Woman"). [avatar] is a newly chosen
 * image to upload, or `null` to leave the current avatar untouched.
 */
data class ProfileUpdate(
    val nickname: String,
    val age: Int?,
    val gender: String?,
    val bio: String?,
    val avatar: AvatarUpload?,
)

/**
 * A new profile photo to upload, as the raw image [bytes] plus the metadata a multipart file
 * part needs. Reading the platform image into this payload is the UI layer's job (see
 * `AvatarImageLoader`), keeping the data layer free of Android URI/`ContentResolver` types.
 */
class AvatarUpload(
    val bytes: ByteArray,
    val mimeType: String,
    val filename: String,
) {
    // Value semantics over the bytes, so a ProfileUpdate carrying an avatar compares
    // structurally (used by tests and to dedupe identical updates).
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AvatarUpload) return false

        return mimeType == other.mimeType &&
            filename == other.filename &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + filename.hashCode()
        return result
    }
}
