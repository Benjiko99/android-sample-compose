package uno.lux.sample.feed.data

import uno.lux.sample.post.Post
import uno.lux.sample.user.User

interface FeedDataSource {
    suspend fun fetch(cursor: String?): FeedPage
}

data class FeedPage(
    val posts: List<Post>,
    val users: List<User>,
    val nextCursor: String?,
    val hasMore: Boolean,
)
