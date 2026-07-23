package uno.lux.sample.profile.data

import uno.lux.sample.post.Post
import uno.lux.sample.user.User
import uno.lux.sample.user.UserId

interface ProfileDataSource {
    suspend fun refresh(userId: UserId): ProfileRefreshData
    suspend fun loadMorePosts(userId: UserId, cursor: String?): PostsPage

    /**
     * A page of the posts [userId] saved. Fetched separately from [refresh] because the Saved
     * tab is private to its owner and loads only once they open it — a profile view never
     * requests it, and the server refuses it for anyone but the signed-in user.
     */
    suspend fun bookmarks(userId: UserId, cursor: String?): PostsWithAuthorsPage

    /**
     * A page of the posts [userId] liked. Public, unlike [bookmarks] — anyone may read anyone's
     * — but loaded on the same on-demand terms, since it is a request the Posts tab doesn't need.
     */
    suspend fun likes(userId: UserId, cursor: String?): PostsWithAuthorsPage
}

data class ProfileRefreshData(
    val postsCount: Int,
    val posts: List<Post>,
    val postCursor: String?,
    val postHasMore: Boolean,
)

data class PostsPage(val posts: List<Post>, val cursor: String?, val hasMore: Boolean)

/**
 * A page of posts for the Saved and Likes tabs. Unlike [PostsPage], it carries [users]: a
 * profile's own posts are all by the profile's user, but a saved or liked one is by an arbitrary
 * author the caller may not know yet.
 */
data class PostsWithAuthorsPage(
    val posts: List<Post>,
    val users: List<User>,
    val cursor: String?,
    val hasMore: Boolean,
)
