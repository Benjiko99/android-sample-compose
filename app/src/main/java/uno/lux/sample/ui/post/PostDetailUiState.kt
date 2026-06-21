package uno.lux.sample.ui.post

import uno.lux.sample.data.Comment
import uno.lux.sample.data.Post

sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState
    data object NotFound : PostDetailUiState
    data class Loaded(
        val post: Post,
        val comments: List<Comment>,
    ) : PostDetailUiState
}
