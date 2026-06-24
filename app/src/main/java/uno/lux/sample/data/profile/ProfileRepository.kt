package uno.lux.sample.data.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import uno.lux.sample.data.SampleAlbums
import uno.lux.sample.data.SampleVideos
import uno.lux.sample.data.post.Album
import uno.lux.sample.data.post.InMemoryPostRepository
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.user.UserId

/**
 * Source of truth for a user's profile metadata and the ordered IDs of their posts.
 *
 * [profile] streams albums, videos, and counts for a given user. [postIds] streams the ordered
 * list of post IDs authored by that user; callers resolve them via [PostRepository.entities] so
 * mutations (likes, bookmarks) are reflected without any involvement from this repository.
 * Post mutations go directly through [PostRepository] — not through here.
 *
 * [hasMorePosts], [hasMoreAlbums], [hasMoreVideos] signal whether another page exists for each
 * tab. The corresponding [loadMore*] methods append the next page into the flows above.
 */
interface ProfileRepository {
    fun profile(userId: UserId): Flow<Profile>
    fun postIds(userId: UserId): Flow<List<PostId>>
    fun hasMorePosts(userId: UserId): Flow<Boolean>
    fun hasMoreAlbums(userId: UserId): Flow<Boolean>
    fun hasMoreVideos(userId: UserId): Flow<Boolean>
    suspend fun refresh(userId: UserId)
    suspend fun loadMorePosts(userId: UserId)
    suspend fun loadMoreAlbums(userId: UserId)
    suspend fun loadMoreVideos(userId: UserId)
}

/**
 * In-memory [ProfileRepository]. Post IDs are derived reactively from the shared entity store so
 * a like toggle on the home feed is immediately reflected on the profile tab — no extra wiring
 * needed. Albums and videos come from static sample maps. There is only one page of sample data
 * so all [hasMore*] flows are always false and [loadMore*] methods are no-ops.
 */
class InMemoryProfileRepository(
    private val postRepository: PostRepository = InMemoryPostRepository(),
    private val albumsByUser: Map<UserId, List<Album>> = SampleAlbums,
    private val videosByUser: Map<UserId, List<Video>> = SampleVideos,
) : ProfileRepository {

    override fun profile(userId: UserId): Flow<Profile> =
        postRepository.entities.map { entities ->
            val userAlbums = albumsByUser[userId].orEmpty()
            val userVideos = videosByUser[userId].orEmpty()

            Profile(
                userId = userId,
                albums = userAlbums,
                videos = userVideos,
                postsCount = entities.values.count { it.authorId == userId },
                albumsCount = userAlbums.size,
                videosCount = userVideos.size,
            )
        }

    override fun postIds(userId: UserId): Flow<List<PostId>> =
        postRepository.entities.map { entities ->
            entities.values
                .filter { it.authorId == userId }
                .sortedByDescending { it.createdAt }
                .map { it.id }
        }

    override fun hasMorePosts(userId: UserId): Flow<Boolean> = flowOf(false)
    override fun hasMoreAlbums(userId: UserId): Flow<Boolean> = flowOf(false)
    override fun hasMoreVideos(userId: UserId): Flow<Boolean> = flowOf(false)

    override suspend fun refresh(userId: UserId) {}
    override suspend fun loadMorePosts(userId: UserId) {}
    override suspend fun loadMoreAlbums(userId: UserId) {}
    override suspend fun loadMoreVideos(userId: UserId) {}
}
