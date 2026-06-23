package uno.lux.sample.data.network

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.profile.Profile
import uno.lux.sample.data.profile.ProfileRepository

class NetworkProfileRepository(
    private val api: MosaicApi,
) : ProfileRepository {

    private val _profiles = MutableStateFlow<Map<String, Profile>>(emptyMap())

    override fun profile(userId: String): Flow<Profile> =
        _profiles.map { it[userId] ?: emptyProfile(userId) }

    override suspend fun refresh(userId: String) = coroutineScope {
        val statsDeferred = async { api.getProfileStats(userId).data }
        val postsDeferred = async { api.getUserPosts(userId).data }
        val albumPostsDeferred = async { api.getUserPosts(userId, type = "photo").data }
        val videoPostsDeferred = async { api.getUserPosts(userId, type = "video").data }

        val stats = statsDeferred.await()
        val posts = postsDeferred.await().map { it.toDomain() }
        val albums = albumPostsDeferred.await().mapNotNull { it.album?.toDomain() }
        val videos = videoPostsDeferred.await().mapNotNull { it.video?.toDomain() }

        _profiles.update {
            it + (userId to Profile(
                userId = userId,
                posts = posts,
                albums = albums,
                videos = videos,
                postsCount = stats.postsCount,
                albumsCount = stats.albumsCount,
                videosCount = stats.videosCount,
            ))
        }
    }

    override suspend fun toggleLike(postId: String) {
        val result = api.toggleLike(postId, EmptyBody()).data
        _profiles.update { map ->
            map.mapValues { (_, profile) ->
                profile.copy(posts = profile.posts.map { post ->
                    if (post.id == postId) post.copy(isLiked = result.isLiked, likeCount = result.likeCount)
                    else post
                })
            }
        }
    }

    override suspend fun toggleBookmark(postId: String) {
        val result = api.toggleBookmark(postId, EmptyBody()).data
        _profiles.update { map ->
            map.mapValues { (_, profile) ->
                profile.copy(posts = profile.posts.map { post ->
                    if (post.id == postId) post.copy(isBookmarked = result.isBookmarked)
                    else post
                })
            }
        }
    }

    private fun emptyProfile(userId: String) = Profile(
        userId = userId,
        posts = emptyList(),
        albums = emptyList(),
        videos = emptyList(),
        postsCount = 0,
        albumsCount = 0,
        videosCount = 0,
    )
}
