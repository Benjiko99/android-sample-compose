package uno.lux.sample.data.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uno.lux.sample.data.SampleAlbums
import uno.lux.sample.data.SampleVideos
import uno.lux.sample.data.post.Album
import uno.lux.sample.data.post.InMemoryPostRepository
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.post.Video

/**
 * Source of truth for a user's profile metadata and the ordered IDs of their posts.
 *
 * [profile] streams albums, videos, and counts for a given user. [postIds] streams the ordered
 * list of post IDs authored by that user; callers resolve them via [PostRepository.entities] so
 * mutations (likes, bookmarks) are reflected without any involvement from this repository.
 * Post mutations go directly through [PostRepository] — not through here.
 */
interface ProfileRepository {
    fun profile(userId: String): Flow<Profile>
    fun postIds(userId: String): Flow<List<String>>
    suspend fun refresh(userId: String)
}

/**
 * In-memory [ProfileRepository]. Post IDs are derived reactively from the shared entity store so
 * a like toggle on the home feed is immediately reflected on the profile tab — no extra wiring
 * needed. Albums and videos come from static sample maps.
 */
class InMemoryProfileRepository(
    private val postRepository: PostRepository = InMemoryPostRepository(),
    private val albumsByUser: Map<String, List<Album>> = SampleAlbums,
    private val videosByUser: Map<String, List<Video>> = SampleVideos,
) : ProfileRepository {

    override fun profile(userId: String): Flow<Profile> =
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

    override fun postIds(userId: String): Flow<List<String>> =
        postRepository.entities.map { entities ->
            entities.values
                .filter { it.authorId == userId }
                .sortedByDescending { it.createdAt }
                .map { it.id }
        }

    override suspend fun refresh(userId: String) {}
}
