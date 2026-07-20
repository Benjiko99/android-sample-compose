package uno.lux.sample.data.profile

import uno.lux.sample.data.post.Post
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserId

interface ProfileDataSource {
    suspend fun refresh(userId: UserId): ProfileRefreshData
    suspend fun loadMorePosts(userId: UserId, cursor: String?): PostsPage

    /**
     * A page of the posts [userId] saved. Fetched separately from [refresh] because the Saved
     * tab is private to its owner and loads only once they open it — a profile view never
     * requests it, and the server refuses it for anyone but the signed-in user.
     */
    suspend fun bookmarks(userId: UserId, cursor: String?): BookmarksPage
}

data class ProfileRefreshData(
    val postsCount: Int,
    val posts: List<Post>,
    val postCursor: String?,
    val postHasMore: Boolean,
)

data class PostsPage(val posts: List<Post>, val cursor: String?, val hasMore: Boolean)

/**
 * A page of saved posts. Unlike [PostsPage], it carries [users]: a profile's own posts are all
 * by the profile's user, but saved ones are by arbitrary authors the caller may not know yet.
 */
data class BookmarksPage(
    val posts: List<Post>,
    val users: List<User>,
    val cursor: String?,
    val hasMore: Boolean,
)
