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
 * Single source of truth for a user's profile content.
 *
 * [profile] streams the [Profile] for a given user id, recomputing whenever the underlying
 * posts change so likes and bookmarks stay live. Whether the user actually exists is the
 * concern of [UserRepository] — profile always returns content (possibly empty) for any id.
 * [refresh] forces a re-fetch from the backing source. The toggle functions express viewer
 * intent on a post, mirroring [PostRepository].
 *
 * The interface is the seam a real network- or database-backed implementation slots into later.
 */
interface ProfileRepository {
    fun profile(userId: String): Flow<Profile>

    suspend fun refresh(userId: String)
    suspend fun toggleLike(postId: String)
    suspend fun toggleBookmark(postId: String)
}

/**
 * In-memory [ProfileRepository]. It composes a [PostRepository] for the reactive Posts tab —
 * reusing that mutation logic rather than duplicating it — and layers the static album/video
 * stand-ins on top. A profile's posts are simply the feed posts whose [authorId] matches.
 */
class InMemoryProfileRepository(
    private val postRepository: PostRepository = InMemoryPostRepository(),
    private val albumsByUser: Map<String, List<Album>> = SampleAlbums,
    private val videosByUser: Map<String, List<Video>> = SampleVideos,
) : ProfileRepository {

    override fun profile(userId: String): Flow<Profile> =
        postRepository.posts.map { posts ->
            val userPosts = posts.filter { it.authorId == userId }
            val userAlbums = albumsByUser[userId].orEmpty()
            val userVideos = videosByUser[userId].orEmpty()

            Profile(
                userId = userId,
                posts = userPosts,
                albums = userAlbums,
                videos = userVideos,
                postsCount = userPosts.size,
                albumsCount = userAlbums.size,
                videosCount = userVideos.size,
            )
        }

    override suspend fun refresh(userId: String) {}

    override suspend fun toggleLike(postId: String) = postRepository.toggleLike(postId)
    override suspend fun toggleBookmark(postId: String) = postRepository.toggleBookmark(postId)
}
