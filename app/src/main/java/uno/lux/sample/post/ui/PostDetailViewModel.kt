package uno.lux.sample.post.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import uno.lux.sample.comment.Comment
import uno.lux.sample.comment.CommentId
import uno.lux.sample.comment.data.CommentRepository
import uno.lux.sample.post.Post
import uno.lux.sample.post.PostId
import uno.lux.sample.post.data.PostRepository
import uno.lux.sample.post.PostWithAuthor
import uno.lux.sample.video.Video
import uno.lux.sample.user.User
import uno.lux.sample.user.UserId
import uno.lux.sample.user.data.UserRepository
import uno.lux.sample.app.di.CurrentUser
import uno.lux.sample.app.navigation.Navigator
import uno.lux.sample.app.navigation.Screen
import uno.lux.sample.app.util.AppError
import uno.lux.sample.app.util.ignoreErrors
import uno.lux.sample.app.util.launchIfIdle
import uno.lux.sample.app.util.stateInWhileSubscribed
import uno.lux.sample.app.util.toAppError

/**
 * Holds the state for a single post's detail view. The post itself comes from the shared
 * [PostRepository] entity store so likes toggled here propagate to the feed and profile screens.
 * Comments are owned by this ViewModel: they are loaded once on creation and updated in-place by
 * [addComment] and [onToggleCommentLike], then discarded when the ViewModel is cleared. This
 * avoids the cross-post keying and memory-retention problems that a shared comment store would
 * introduce.
 *
 * The entity store normally already holds the post — this page is reached from a feed or profile
 * that fetched it. It does not when the page is restored after **process death**, where the back
 * stack comes back but every in-memory store starts empty, so [load] fetches the post it was
 * given the ID for. That fetch is why an absent post can't simply mean [PostDetailUiState.NotFound]:
 * "not asked yet", "the server says it's gone" and "the request failed" are three different
 * screens, and [PostLoad] is what tells them apart.
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

    /**
     * How the fetch for a post the stores don't hold resolved. Only consulted while the stores
     * can't answer — once the post is resolved it is the post that is shown, whatever happened
     * to a request beforehand.
     */
    private sealed interface PostLoad {
        data object Pending : PostLoad
        data object Missing : PostLoad
        data class Failed(val error: AppError) : PostLoad
    }

    private val _fetchOutcome = MutableStateFlow<PostLoad>(PostLoad.Pending)

    /** Whether the store has been told this post is gone, narrowed to the one id this page shows. */
    private val isDeleted: Flow<Boolean> = postRepository.deletedIds
        .map { postId in it }
        .distinctUntilChanged()

    /**
     * The fetch outcome, overridden to [PostLoad.Missing] once the store says the post was
     * deleted — whichever screen deleted it. This page may be sitting under the profile that
     * removed the post, and without the override the emptied store would read as "not loaded
     * yet" and leave this page spinning on a fetch nobody is going to make.
     */
    private val postLoad: Flow<PostLoad> =
        combine(_fetchOutcome, isDeleted) { outcome, deleted ->
            if (deleted) PostLoad.Missing else outcome
        }

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    private val _commentsError = MutableStateFlow<AppError?>(null)
    private val _commentsLoading = MutableStateFlow(true)

    /**
     * The post and its author, resolved together — one is no use without the other. Both stores
     * emit for changes to any post or user, so this only passes on the ones that move *this*
     * pair; without that, a like anywhere in the feed underneath would rebuild the whole state.
     */
    private val postWithAuthor: Flow<PostWithAuthor?> =
        combine(postRepository.entities, userRepository.users) { entities, users ->
            resolve(entities, users)
        }.distinctUntilChanged()

    val uiState: StateFlow<PostDetailUiState> = combine(
        postWithAuthor,
        _comments,
        _commentsError,
        _commentsLoading,
        postLoad,
    ) { resolved, comments, commentsError, commentsLoading, postLoad ->
        when {
            resolved != null -> PostDetailUiState.Loaded(
                post = resolved.post,
                author = resolved.author,
                comments = comments,
                commentsError = commentsError,
                commentsLoading = commentsLoading,
                isOwn = resolved.post.authorId == currentUser.id,
            )
            postLoad is PostLoad.Missing -> PostDetailUiState.NotFound
            postLoad is PostLoad.Failed -> PostDetailUiState.Error(postLoad.error)
            else -> PostDetailUiState.Loading
        }
    }.stateInWhileSubscribed(viewModelScope, PostDetailUiState.Loading)

    /** The signed-in user, drawn as the composer's avatar. */
    val composerUser: User = currentUser

    private var loadJob: Job? = null

    init {
        retry()
    }

    /** Reloads everything the page failed to get — the post itself included. */
    fun retry() = launchIfIdle(::loadJob) {
        coroutineScope {
            launch { loadPost() }
            launch { loadComments() }
        }
    }

    fun retryComments() {
        viewModelScope.launch { loadComments() }
    }

    /**
     * Fetches the post unless the stores already hold it with its author, which they do whenever
     * this page was opened from a feed or profile. The fetch is for the cold start: a detail page
     * restored after process death, where this ViewModel is the first thing to ask for the post.
     */
    private suspend fun loadPost() {
        if (resolve(postRepository.entities.value, userRepository.users.value) != null) return

        _fetchOutcome.value = PostLoad.Pending
        ignoreErrors(
            onError = { e ->
                Timber.w(e)
                _fetchOutcome.value = PostLoad.Failed(e.toAppError())
            },
        ) {
            // A 404 is the server saying the post is gone — an answer, not a failure to retry.
            if (postRepository.load(postId) == null) _fetchOutcome.value = PostLoad.Missing
        }
    }

    /** This post paired with its author, or null while either store is still missing its half. */
    private fun resolve(entities: Map<PostId, Post>, users: Map<UserId, User>): PostWithAuthor? {
        val post = entities[postId] ?: return null
        val author = users[post.authorId] ?: return null

        return PostWithAuthor(post, author)
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
                // The store marks the deletion, and [postLoad] reads NotFound from it — the same
                // path a deletion performed on any other screen takes.
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
