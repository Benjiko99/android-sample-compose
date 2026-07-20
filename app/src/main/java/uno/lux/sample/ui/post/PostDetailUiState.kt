package uno.lux.sample.ui.post

import uno.lux.sample.data.user.User
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.Post
import uno.lux.sample.util.AppError

sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState
    data object NotFound : PostDetailUiState
    /** [isOwn] is the post being authored by the signed-in user; it gates the delete action. */
    data class Loaded(
        val post: Post,
        val author: User,
        val comments: List<Comment>,
        val commentsError: AppError? = null,
        val isOwn: Boolean = false,
    ) : PostDetailUiState
}
