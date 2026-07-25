package uno.lux.sample.feed.data

import uno.lux.sample.post.data.domain.Post
import uno.lux.sample.user.data.domain.User

interface FeedDataSource {
    suspend fun fetch(cursor: String?): FeedPage
}

data class FeedPage(
    val posts: List<Post>,
    val users: List<User>,
    val nextCursor: String?,
    val hasMore: Boolean,
)
