package uno.lux.sample.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import uno.lux.sample.data.user.User
import uno.lux.sample.data.post.CommentRepository
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.di.CurrentUser
import uno.lux.sample.util.stateInWhileSubscribed

/**
 * Holds the state for a single post's detail view, combining the live post from
 * [PostRepository], the resolved author from [UserRepository], and the comment thread from
 * [CommentRepository]. Like, bookmark, and comment-like intent are translated into repository
 * mutations; new comments are prepended.
 *
 * [postId] is a runtime argument wired through [Factory] / assisted injection so every opened
 * post gets its own ViewModel, scoped to its back-stack entry.
 */
@HiltViewModel(assistedFactory = PostDetailViewModel.Factory::class)
class PostDetailViewModel @AssistedInject constructor(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    @CurrentUser private val currentUser: User,
    @Assisted private val postId: String,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(postId: String): PostDetailViewModel
    }

    val uiState: StateFlow<PostDetailUiState> = combine(
        postRepository.posts,
        commentRepository.comments(postId),
        userRepository.users,
    ) { posts, comments, users ->
        val post = posts.find { it.id == postId }
            ?: return@combine PostDetailUiState.NotFound
        val author = users[post.authorId]
            ?: return@combine PostDetailUiState.NotFound
        PostDetailUiState.Loaded(post, author, comments)
    }.stateInWhileSubscribed(viewModelScope, PostDetailUiState.Loading)

    /** Nickname of the signed-in user, shown as the composer's avatar label. */
    val composerUserName: String = currentUser.nickname

    fun onToggleLike() {
        viewModelScope.launch { postRepository.toggleLike(postId) }
    }

    fun onToggleBookmark() {
        viewModelScope.launch { postRepository.toggleBookmark(postId) }
    }

    fun onToggleCommentLike(commentId: String) {
        viewModelScope.launch { commentRepository.toggleLike(postId, commentId) }
    }

    fun addComment(text: String) {
        viewModelScope.launch { commentRepository.addComment(postId, text) }
    }
}
