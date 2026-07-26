package uno.lux.sample.comment.data

import uno.lux.sample.comment.data.domain.Comment
import uno.lux.sample.comment.data.domain.CommentId
import uno.lux.sample.common.data.LikeState
import uno.lux.sample.post.data.domain.PostId

interface CommentDataSource {
    suspend fun loadComments(postId: PostId): List<Comment>

    suspend fun addComment(postId: PostId, text: String): Comment

    /**
     * Puts the viewer's like on [commentId] into [liked] and answers the state the server settled
     * on — the same contract [uno.lux.sample.post.data.PostDataSource.setLike] has, for the same
     * reasons: idempotent, so a retry cannot move the like twice, and carrying only the two
     * fields a like owns, so it has no stale copy of the comment to write back.
     */
    suspend fun setLike(
        postId: PostId,
        commentId: CommentId,
        liked: Boolean,
    ): LikeState
}
