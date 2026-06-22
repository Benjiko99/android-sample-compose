package uno.lux.sample.data.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import uno.lux.sample.data.SampleAlbums
import uno.lux.sample.data.SampleUsers
import uno.lux.sample.data.SampleVideos
import uno.lux.sample.data.User
import uno.lux.sample.data.post.Album
import uno.lux.sample.data.post.InMemoryPostRepository
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.post.Video

/**
 * Single source of truth for a user's profile.
 *
 * [profile] streams the [Profile] for a given user id, recomputing whenever the underlying
 * posts change so likes and bookmarks stay live; it emits `null` for an unknown id. The
 * toggle functions express viewer intent on a post, mirroring [PostRepository]. The interface
 * is the seam a real network- or database-backed implementation slots into later.
 */
interface ProfileRepository {
    fun profile(userId: String): Flow<Profile?>

    suspend fun toggleLike(postId: String)
    suspend fun toggleBookmark(postId: String)
}

/**
 * In-memory [ProfileRepository]. It composes a [PostRepository] for the reactive Posts tab —
 * reusing that mutation logic rather than duplicating it — and layers the static user
 * directory and album/video stand-ins on top. A profile's posts are simply the feed posts
 * authored by that user.
 */
class InMemoryProfileRepository(
    private val users: List<User> = SampleUsers,
    private val postRepository: PostRepository = InMemoryPostRepository(),
    private val albumsByUser: Map<String, List<Album>> = SampleAlbums,
    private val videosByUser: Map<String, List<Video>> = SampleVideos,
) : ProfileRepository {

    override fun profile(userId: String): Flow<Profile?> {
        val user = users.firstOrNull { it.id == userId } ?: return flowOf(null)
        return postRepository.posts.map { posts ->
            val userPosts = posts.filter { it.author.id == userId }
            val userAlbums = albumsByUser[userId].orEmpty()
            val userVideos = videosByUser[userId].orEmpty()
            Profile(
                user = user,
                posts = userPosts,
                albums = userAlbums,
                videos = userVideos,
                postsCount = userPosts.size,
                albumsCount = userAlbums.size,
                videosCount = userVideos.size,
            )
        }
    }

    override suspend fun toggleLike(postId: String) = postRepository.toggleLike(postId)
    override suspend fun toggleBookmark(postId: String) = postRepository.toggleBookmark(postId)
}
