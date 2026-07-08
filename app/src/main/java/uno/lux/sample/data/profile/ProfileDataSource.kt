package uno.lux.sample.data.profile

import uno.lux.sample.data.post.Post
import uno.lux.sample.data.user.UserId

interface ProfileDataSource {
    suspend fun refresh(userId: UserId): ProfileRefreshData
    suspend fun loadMorePosts(userId: UserId, cursor: String?): PostsPage
}

data class ProfileRefreshData(
    val postsCount: Int,
    val posts: List<Post>,
    val postCursor: String?,
    val postHasMore: Boolean,
)

data class PostsPage(val posts: List<Post>, val cursor: String?, val hasMore: Boolean)
