package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import uno.lux.sample.data.network.dto.UserNetworkDto
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserRepository

/**
 * Network-backed [UserRepository]. Holds a normalised user cache so every screen shares one
 * copy of each user. Authors sideloaded in the feed response are pushed in via [ingest];
 * individual users are fetched on demand by [refresh].
 */
class NetworkUserRepository(
    private val api: MosaicApi,
) : UserRepository {

    private val _cache = MutableStateFlow<Map<String, User>>(emptyMap())
    override val users: Flow<Map<String, User>> = _cache.asStateFlow()

    override fun user(userId: String): Flow<User?> = _cache.map { it[userId] }

    override suspend fun refresh(userId: String) = withContext(Dispatchers.IO) {
        val dto = api.getUser(userId)
        _cache.update { it + (userId to dto.data.toDomain()) }
    }

    /** Merges [users] from a feed sideload into the cache without evicting existing entries. */
    fun ingest(users: List<UserNetworkDto>) {
        if (users.isEmpty()) return
        _cache.update { current -> current + users.associate { it.id to it.toDomain() } }
    }
}
