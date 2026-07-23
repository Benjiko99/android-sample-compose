package uno.lux.sample.data.feed

import uno.lux.sample.data.post.Post
import uno.lux.sample.data.user.User

interface FeedDataSource {
    suspend fun fetch(cursor: String?): FeedPage
}

data class FeedPage(
    val posts: List<Post>,
    val users: List<User>,
    val nextCursor: String?,
    val hasMore: Boolean,
)
