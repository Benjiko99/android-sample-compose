package uno.lux.sample.post.data.domain

/**
 * A post's like fields as the server holds them: whether the viewer likes it, and how many
 * likes it has.
 *
 * The two travel together because a like moves both, and they arrive as their own value rather
 * than as a whole [Post] because they are the *only* fields a like request may write back. A
 * request that answered with a post would carry a copy of every other field as the server saw it
 * when the request went out, and writing that copy into the store would undo anything that landed
 * meanwhile.
 */
data class LikeState(
    val isLiked: Boolean,
    val likeCount: Int,
)
