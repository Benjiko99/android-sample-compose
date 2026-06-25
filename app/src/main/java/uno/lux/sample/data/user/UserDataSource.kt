package uno.lux.sample.data.user

interface UserDataSource {
    suspend fun fetch(userId: UserId): User?
}
