package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.profile.Profile
import uno.lux.sample.data.profile.ProfileRepository
import uno.lux.sample.data.user.UserId

class NetworkProfileRepository(
    private val api: MosaicApi,
    private val postRepository: PostRepository,
) : ProfileRepository {

    private val _profiles = MutableStateFlow<Map<UserId, Profile>>(emptyMap())
    private val _userPostIds = MutableStateFlow<Map<UserId, List<PostId>>>(emptyMap())

    override fun profile(userId: UserId): Flow<Profile> =
        _profiles.map { it[userId] ?: emptyProfile(userId) }

    override fun postIds(userId: UserId): Flow<List<PostId>> =
        _userPostIds.map { it[userId] ?: emptyList() }

    override suspend fun refresh(userId: UserId) = withContext(Dispatchers.IO) {
        val statsDeferred = async { api.getProfileStats(userId).data }
        val postsDeferred = async { api.getUserPosts(userId).data }
        val albumPostsDeferred = async { api.getUserPosts(userId, type = "photo").data }
        val videoPostsDeferred = async { api.getUserPosts(userId, type = "video").data }

        val stats = statsDeferred.await()
        val postDtos = postsDeferred.await()
        val albums = albumPostsDeferred.await().mapNotNull { it.album?.toDomain() }
        val videos = videoPostsDeferred.await().mapNotNull { it.video?.toDomain() }

        val posts = postDtos.map { it.toDomain() }
        postRepository.ingest(posts)

        _userPostIds.update { it + (userId to posts.map { p -> p.id }) }
        _profiles.update {
            it + (userId to Profile(
                userId = userId,
                albums = albums,
                videos = videos,
                postsCount = stats.postsCount,
                albumsCount = stats.albumsCount,
                videosCount = stats.videosCount,
            ))
        }
    }

    private fun emptyProfile(userId: UserId) = Profile(
        userId = userId,
        albums = emptyList(),
        videos = emptyList(),
        postsCount = 0,
        albumsCount = 0,
        videosCount = 0,
    )
}
