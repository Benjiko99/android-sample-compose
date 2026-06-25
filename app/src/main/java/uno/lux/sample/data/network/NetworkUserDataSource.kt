package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserDataSource
import uno.lux.sample.data.user.UserId

class NetworkUserDataSource(
    private val api: MosaicApi,
) : UserDataSource {

    override suspend fun fetch(userId: UserId): User? = withContext(Dispatchers.IO) {
        api.getUser(userId).data.toDomain()
    }
}
