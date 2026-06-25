package uno.lux.sample.data.user

import uno.lux.sample.data.SampleUsers

class LocalUserDataSource : UserDataSource {

    private val users = SampleUsers.associateBy { it.id }

    override suspend fun fetch(userId: UserId): User? = users[userId]
}
