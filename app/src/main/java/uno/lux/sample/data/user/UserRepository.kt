package uno.lux.sample.data.user

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import uno.lux.sample.data.SampleUsers
import uno.lux.sample.data.user.User

/**
 * Single source of truth for user identity.
 *
 * [user] streams the cached [User] for a given id, emitting `null` when the id is unknown.
 * [users] streams the full map of all currently known users keyed by id — used by list
 * screens (the feed, a profile's posts tab) to resolve author names without a per-row lookup.
 * [refresh] forces a re-fetch of a single entry from the backing source, replacing the cache.
 *
 * The interface is the seam a real network-backed implementation slots into later.
 */
interface UserRepository {
    fun user(userId: String): Flow<User?>
    val users: Flow<Map<String, User>>
    suspend fun refresh(userId: String)
}

/**
 * In-memory [UserRepository] pre-populated with all [allUsers]. The cache is read-only in
 * the in-memory case — [refresh] is a no-op because the data never changes.
 */
class InMemoryUserRepository(
    allUsers: List<User> = SampleUsers,
) : UserRepository {

    private val cache = MutableStateFlow(allUsers.associateBy { it.id })

    override val users: Flow<Map<String, User>> = cache.asStateFlow()

    override fun user(userId: String): Flow<User?> = cache.map { it[userId] }

    override suspend fun refresh(userId: String) {}
}
