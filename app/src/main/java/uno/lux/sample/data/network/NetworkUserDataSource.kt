package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import uno.lux.sample.data.user.ProfileUpdate
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserDataSource
import uno.lux.sample.data.user.UserId

class NetworkUserDataSource(
    private val api: MosaicApi,
) : UserDataSource {

    override suspend fun fetch(userId: UserId): User? = withContext(Dispatchers.IO) {
        api.getUser(userId).data.toDomain()
    }

    override suspend fun update(userId: UserId, update: ProfileUpdate): User =
        withContext(Dispatchers.IO) {
            // Empty text parts clear the nullable fields server-side (multipart can't carry a
            // JSON null); the avatar part is sent only when a new image was chosen.
            val avatarPart = update.avatar?.let { avatar ->
                MultipartBody.Part.createFormData(
                    name = "avatar",
                    filename = avatar.filename,
                    body = avatar.bytes.toRequestBody(avatar.mimeType.toMediaType()),
                )
            }

            api.updateUser(
                id = userId,
                nickname = textPart(update.nickname),
                age = textPart(update.age?.toString().orEmpty()),
                gender = textPart(update.gender.orEmpty()),
                bio = textPart(update.bio.orEmpty()),
                avatar = avatarPart,
            ).data.toDomain()
        }

    override suspend fun toggleFollow(user: User): User = withContext(Dispatchers.IO) {
        val result = api.toggleFollow(user.id).data
        user.copy(isFollowing = result.isFollowing, followerCount = result.followerCount)
    }

    private fun textPart(value: String) = value.toRequestBody(PLAIN_TEXT)

    private companion object {
        val PLAIN_TEXT = "text/plain".toMediaType()
    }
}
