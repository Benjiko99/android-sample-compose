package uno.lux.sample.ui.post

import uno.lux.sample.data.user.User
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.Post

sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState
    data object NotFound : PostDetailUiState
    data class Loaded(
        val post: Post,
        val author: User,
        val comments: List<Comment>,
        val commentsLoadFailed: Boolean = false,
    ) : PostDetailUiState
}
