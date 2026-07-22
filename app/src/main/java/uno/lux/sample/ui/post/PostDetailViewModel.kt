package uno.lux.sample.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.CommentId
import uno.lux.sample.data.post.CommentRepository
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserId
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.di.CurrentUser
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.navigation.Screen
import uno.lux.sample.util.AppError
import uno.lux.sample.util.ignoreErrors
import uno.lux.sample.util.stateInWhileSubscribed

/**
 * Holds the state for a single post's detail view. The post itself comes from the shared
 * [PostRepository] entity store so likes toggled here propagate to the feed and profile screens.
 * Comments are owned by this ViewModel: they are loaded once on creation and updated in-place by
 * [addComment] and [onToggleCommentLike], then discarded when the ViewModel is cleared. This
 * avoids the cross-post keying and memory-retention problems that a shared comment store would
 * introduce.
 *
 * Navigation intents (going back, opening the author or a media viewer) are pushes and pops on
 * the injected [Navigator]. [postId] is a runtime argument wired through [Factory] / assisted
 * injection so every opened post gets its own ViewModel, scoped to its back-stack entry.
 */
@HiltViewModel(assistedFactory = PostDetailViewModel.Factory::class)
class PostDetailViewModel @AssistedInject constructor(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val navigator: Navigator,
    @param:CurrentUser private val currentUser: User,
    @Assisted private val postId: PostId,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(postId: PostId): PostDetailViewModel
    }

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    private val _commentsError = MutableStateFlow<AppError?>(null)
    private val _commentsLoading = MutableStateFlow(true)

    val uiState: StateFlow<PostDetailUiState> = combine(
        postRepository.entities.map { it[postId] },
        _comments,
        userRepository.users,
        _commentsError,
        _commentsLoading,
    ) { post, comments, users, commentsError, commentsLoading ->
        if (post == null) return@combine PostDetailUiState.NotFound
        val author = users[post.authorId] ?: return@combine PostDetailUiState.NotFound
        PostDetailUiState.Loaded(
            post = post,
            author = author,
            comments = comments,
            commentsError = commentsError,
            commentsLoading = commentsLoading,
            isOwn = post.authorId == currentUser.id,
        )
    }.stateInWhileSubscribed(viewModelScope, PostDetailUiState.Loading)

    /** The signed-in user, drawn as the composer's avatar. */
    val composerUser: User = currentUser

    init {
        retryComments()
    }

    fun retryComments() {
        viewModelScope.launch { loadComments() }
    }

    private suspend fun loadComments() {
        _commentsError.value = null
        _commentsLoading.value = true

        try {
            ignoreErrors(_commentsError) {
                _comments.value = commentRepository.loadComments(postId)
            }
        } finally {
            _commentsLoading.value = false
        }
    }

    fun onToggleLike() {
        viewModelScope.launch {
            ignoreErrors { postRepository.toggleLike(postId) }
        }
    }

    fun onToggleBookmark() {
        viewModelScope.launch {
            ignoreErrors { postRepository.toggleBookmark(postId) }
        }
    }

    /**
     * Deletes the post and pops this screen. Backing out is part of the outcome, not the caller's
     * follow-up: once the entity is gone the detail view has nothing left to show, and the feed or
     * profile underneath has already dropped the post through the shared entity store.
     */
    fun onDelete() {
        viewModelScope.launch {
            ignoreErrors {
                postRepository.delete(postId)
                navigator.goBack()
            }
        }
    }

    fun onToggleCommentLike(commentId: CommentId) {
        viewModelScope.launch {
            val comment = _comments.value.find { it.id == commentId } ?: return@launch
            ignoreErrors {
                val updated = commentRepository.toggleLike(postId, comment)
                _comments.update { current -> current.map { if (it.id == commentId) updated else it } }
            }
        }
    }

    fun addComment(text: String) {
        viewModelScope.launch {
            ignoreErrors {
                val comment = commentRepository.addComment(postId, text)
                _comments.update { listOf(comment) + it }
            }
        }
    }

    fun goBack() = navigator.goBack()

    fun openProfile(userId: UserId) = navigator.goTo(Screen.Profile(userId))

    fun openVideo(video: Video) = navigator.goTo(Screen.FullscreenVideo(video))

    fun openAlbum(imageUrls: List<String>, initialIndex: Int) =
        navigator.goTo(Screen.AlbumViewer(imageUrls, initialIndex))
}
