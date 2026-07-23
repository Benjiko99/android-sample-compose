package uno.lux.sample.ui.post

import uno.lux.sample.data.user.User
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.Post
import uno.lux.sample.util.AppError

sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState

    /** The server answered that there is no such post — it never existed, or it was deleted. */
    data object NotFound : PostDetailUiState

    /**
     * The post could not be fetched. Distinct from [NotFound]: nothing is known about the post
     * yet, so the screen offers a retry rather than telling the user it is gone.
     */
    data class Error(val error: AppError) : PostDetailUiState

    /** [isOwn] is the post being authored by the signed-in user; it gates the delete action. */
    data class Loaded(
        val post: Post,
        val author: User,
        val comments: List<Comment>,
        val commentsError: AppError? = null,
        val commentsLoading: Boolean = false,
        val isOwn: Boolean = false,
    ) : PostDetailUiState
}
