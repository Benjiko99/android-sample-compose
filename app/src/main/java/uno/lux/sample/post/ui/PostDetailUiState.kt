package uno.lux.sample.post.ui

import uno.lux.sample.app.util.AppError
import uno.lux.sample.comment.data.domain.Comment
import uno.lux.sample.comment.data.domain.CommentId
import uno.lux.sample.post.data.domain.Post
import uno.lux.sample.user.data.domain.User

/**
 * How far the comment in the composer has got. The composer owns the text itself, so this is
 * what tells it when to give the text up: [SENT] is reached only once the server has taken the
 * comment, and a failure returns to [IDLE] with what the user typed still in the box.
 *
 * [SENDING] disables the field and the send button, which is what makes the send a single
 * outcome — without it a second tap would post the comment twice, and clearing the box up front
 * would throw the text away on a send that never landed.
 */
enum class CommentSendState {
    IDLE,
    SENDING,
    SENT,
}

sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState

    /** The server answered that there is no such post — it never existed, or it was deleted. */
    data object NotFound : PostDetailUiState

    /**
     * The post could not be fetched. Distinct from [NotFound]: nothing is known about the post
     * yet, so the screen offers a retry rather than telling the user it is gone.
     */
    data class Error(
        val error: AppError,
    ) : PostDetailUiState

    /**
     * [comments] is the stretch of the thread loaded so far, not the whole of it —
     * [commentsEndReached] is what says whether there is more below, and the count in the header
     * comes from the post rather than from this list. [isOwn] is the post being authored by the
     * signed-in user; it gates the delete action.
     *
     * [scrollToComment] is a comment the screen has yet to bring into view — the one the user just
     * sent. It is spent once acted on, through `onScrolledToComment`, so a configuration change
     * cannot yank the list back a second time. [commentSend] is spent the same way, through
     * `onCommentSent`, once the composer has cleared the text the server took.
     */
    data class Loaded(
        val post: Post,
        val author: User,
        val comments: List<Comment>,
        val commentsError: AppError? = null,
        val commentsLoading: Boolean = false,
        val commentsEndReached: Boolean = true,
        val isOwn: Boolean = false,
        val scrollToComment: CommentId? = null,
        val commentSend: CommentSendState = CommentSendState.IDLE,
    ) : PostDetailUiState
}
