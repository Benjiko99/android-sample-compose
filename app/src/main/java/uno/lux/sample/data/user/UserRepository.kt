package uno.lux.sample.data.user

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Single source of truth for user identity.
 *
 * [user] streams the cached [User] for a given id, emitting `null` when the id is unknown.
 * [users] streams the full map of all currently known users keyed by id — used by list
 * screens (the feed, a profile's posts tab) to resolve author names without a per-row lookup.
 * [ingest] merges a batch of users (e.g. sideloaded from the feed response) into the cache.
 * [refresh] forces a re-fetch of a single entry from the backing source, replacing the cache.
 *
 * Where the data comes from — in-memory sample data vs. a live network call — is decided by
 * [UserDataSource].
 */
class UserRepository(
    private val dataSource: UserDataSource,
) {
    private val _cache = MutableStateFlow<Map<UserId, User>>(emptyMap())
    val users: Flow<Map<UserId, User>> = _cache.asStateFlow()

    fun user(userId: UserId): Flow<User?> = _cache.map { it[userId] }

    fun ingest(users: List<User>) {
        if (users.isEmpty()) return
        _cache.update { current -> current + users.associateBy { it.id } }
    }

    suspend fun refresh(userId: UserId) {
        val user = dataSource.fetch(userId) ?: return
        _cache.update { it + (userId to user) }
    }
}
