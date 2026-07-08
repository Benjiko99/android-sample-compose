package uno.lux.sample.ui.post

import uno.lux.sample.data.user.User
import uno.lux.sample.data.post.Post

/** Resolved display data for a single post card: the post and its resolved author. */
data class PostCardData(
    val post: Post,
    val author: User,
)
