package uno.lux.sample.data.network

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.profile.Profile
import uno.lux.sample.data.profile.ProfileRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Network-backed [ProfileRepository]. Each user's profile is loaded lazily on the first
 * [refresh] call and stored as a [MutableStateFlow] so the UI reacts to mutations.
 *
 * Posts, albums (photo posts), and videos (video posts) are each fetched in parallel from the
 * per-user posts endpoint. The profile's tab lists are populated from the first page — full
 * cursor pagination is tracked in [PLAN.md] as a follow-up task.
 */
class NetworkProfileRepository(
    private val api: SampleApi,
) : ProfileRepository {

    private val profiles = ConcurrentHashMap<String, MutableStateFlow<Profile>>()

    private fun profileState(userId: String): MutableStateFlow<Profile> =
        profiles.getOrPut(userId) {
            MutableStateFlow(Profile(userId, emptyList(), emptyList(), emptyList(), 0, 0, 0))
        }

    override fun profile(userId: String): Flow<Profile> = profileState(userId).asStateFlow()

    override suspend fun refresh(userId: String) = coroutineScope {
        val statsDeferred = async { api.getProfileStats(userId).data }
        val postsDeferred = async { api.getUserPosts(userId).data }
        val albumPostsDeferred = async { api.getUserPosts(userId, type = "photo").data }
        val videoPostsDeferred = async { api.getUserPosts(userId, type = "video").data }

        val stats = statsDeferred.await()
        val posts = postsDeferred.await().map { it.toDomain() }
        val albums = albumPostsDeferred.await().mapNotNull { it.album?.toDomain() }
        val videos = videoPostsDeferred.await().mapNotNull { it.video?.toDomain() }

        profileState(userId).value = Profile(
            userId = userId,
            posts = posts,
            albums = albums,
            videos = videos,
            postsCount = stats.postsCount,
            albumsCount = stats.albumsCount,
            videosCount = stats.videosCount,
        )
    }

    override suspend fun toggleLike(postId: String) {
        val result = api.toggleLike(postId, EmptyBody()).data
        updatePostInAllProfiles(postId) { post ->
            post.copy(isLiked = result.isLiked, likeCount = result.likeCount)
        }
    }

    override suspend fun toggleBookmark(postId: String) {
        val result = api.toggleBookmark(postId, EmptyBody()).data
        updatePostInAllProfiles(postId) { post ->
            post.copy(isBookmarked = result.isBookmarked)
        }
    }

    private fun updatePostInAllProfiles(postId: String, transform: (Post) -> Post) {
        profiles.values.forEach { state ->
            state.update { profile ->
                val updated = profile.posts.map { if (it.id == postId) transform(it) else it }
                profile.copy(posts = updated)
            }
        }
    }
}
