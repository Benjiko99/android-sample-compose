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
            // A new avatar upload yields a fresh hosted URL; no upload leaves it unchanged.
            avatarUrl = if (update.avatar != null) UPLOADED_AVATAR_URL else base.avatarUrl,
        )
    }

    companion object {
        const val UPLOADED_AVATAR_URL = "https://cdn.test/avatars/uploaded.png"
    }
}
