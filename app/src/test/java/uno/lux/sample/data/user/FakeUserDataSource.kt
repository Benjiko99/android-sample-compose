package uno.lux.sample.data.user

internal class FakeUserDataSource(private val users: Map<UserId, User> = emptyMap()) : UserDataSource {
    override suspend fun fetch(userId: UserId): User? = users[userId]
}
