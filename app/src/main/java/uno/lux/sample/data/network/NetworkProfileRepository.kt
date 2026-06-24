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

    private data class PageState(val cursor: String?, val hasMore: Boolean)

    private val _postPage = MutableStateFlow<Map<UserId, PageState>>(emptyMap())
    private val _albumPage = MutableStateFlow<Map<UserId, PageState>>(emptyMap())
    private val _videoPage = MutableStateFlow<Map<UserId, PageState>>(emptyMap())

    override fun profile(userId: UserId): Flow<Profile> =
        _profiles.map { it[userId] ?: emptyProfile(userId) }

    override fun postIds(userId: UserId): Flow<List<PostId>> =
        _userPostIds.map { it[userId] ?: emptyList() }

    override fun hasMorePosts(userId: UserId): Flow<Boolean> =
        _postPage.map { it[userId]?.hasMore ?: false }

    override fun hasMoreAlbums(userId: UserId): Flow<Boolean> =
        _albumPage.map { it[userId]?.hasMore ?: false }

    override fun hasMoreVideos(userId: UserId): Flow<Boolean> =
        _videoPage.map { it[userId]?.hasMore ?: false }

    override suspend fun refresh(userId: UserId) = withContext(Dispatchers.IO) {
        val statsDeferred = async { api.getProfileStats(userId).data }
        val postsDeferred = async { api.getUserPosts(userId) }
        val albumPostsDeferred = async { api.getUserPosts(userId, type = "photo") }
        val videoPostsDeferred = async { api.getUserPosts(userId, type = "video") }

        val stats = statsDeferred.await()
        val postsResponse = postsDeferred.await()
        val albumsResponse = albumPostsDeferred.await()
        val videosResponse = videoPostsDeferred.await()

        val posts = postsResponse.data.map { it.toDomain() }
        val albums = albumsResponse.data.mapNotNull { it.album?.toDomain() }
        val videos = videosResponse.data.mapNotNull { it.video?.toDomain() }

        postRepository.ingest(posts)

        _postPage.update { it + (userId to PageState(postsResponse.page.nextCursor, postsResponse.page.hasMore)) }
        _albumPage.update { it + (userId to PageState(albumsResponse.page.nextCursor, albumsResponse.page.hasMore)) }
        _videoPage.update { it + (userId to PageState(videosResponse.page.nextCursor, videosResponse.page.hasMore)) }

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

    override suspend fun loadMorePosts(userId: UserId) = withContext(Dispatchers.IO) {
        val page = _postPage.value[userId] ?: return@withContext
        if (!page.hasMore) return@withContext

        val response = api.getUserPosts(userId, cursor = page.cursor)
        val posts = response.data.map { it.toDomain() }
        postRepository.ingest(posts)

        _postPage.update { it + (userId to PageState(response.page.nextCursor, response.page.hasMore)) }
        _userPostIds.update { map ->
            val existing = map[userId] ?: emptyList()
            map + (userId to existing + posts.map { it.id })
        }
    }

    override suspend fun loadMoreAlbums(userId: UserId) = withContext(Dispatchers.IO) {
        val page = _albumPage.value[userId] ?: return@withContext
        if (!page.hasMore) return@withContext

        val response = api.getUserPosts(userId, type = "photo", cursor = page.cursor)
        val newAlbums = response.data.mapNotNull { it.album?.toDomain() }

        _albumPage.update { it + (userId to PageState(response.page.nextCursor, response.page.hasMore)) }
        _profiles.update { map ->
            val existing = map[userId] ?: return@update map
            map + (userId to existing.copy(albums = existing.albums + newAlbums))
        }
    }

    override suspend fun loadMoreVideos(userId: UserId) = withContext(Dispatchers.IO) {
        val page = _videoPage.value[userId] ?: return@withContext
        if (!page.hasMore) return@withContext

        val response = api.getUserPosts(userId, type = "video", cursor = page.cursor)
        val newVideos = response.data.mapNotNull { it.video?.toDomain() }

        _videoPage.update { it + (userId to PageState(response.page.nextCursor, response.page.hasMore)) }
        _profiles.update { map ->
            val existing = map[userId] ?: return@update map
            map + (userId to existing.copy(videos = existing.videos + newVideos))
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
