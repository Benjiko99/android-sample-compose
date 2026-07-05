package uno.lux.sample.data.user

import java.net.ConnectException

internal class FakeUserDataSource(
    private val users: Map<UserId, User> = emptyMap(),
    var failUpdates: Boolean = false,
) : UserDataSource {

    var lastUpdate: Pair<UserId, ProfileUpdate>? = null
        private set

    override suspend fun fetch(userId: UserId): User? = users[userId]

    override suspend fun update(userId: UserId, update: ProfileUpdate): User {
        if (failUpdates) throw ConnectException("FakeUserDataSource: update failed")
        lastUpdate = userId to update

        val base = users[userId] ?: error("FakeUserDataSource: no user for id '$userId'")
        return base.copy(
            nickname = update.nickname,
            age = update.age,
            gender = update.gender,
            bio = update.bio,
            avatarUrl = update.avatarUrl,
        )
    }
}
