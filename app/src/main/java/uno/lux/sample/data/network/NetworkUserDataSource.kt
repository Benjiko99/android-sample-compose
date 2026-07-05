package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            api.updateUser(userId, update.toDto()).data.toDomain()
        }
}
