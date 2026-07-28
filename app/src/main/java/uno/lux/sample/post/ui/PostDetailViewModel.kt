package uno.lux.sample.post.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uno.lux.sample.app.di.CurrentUser
import uno.lux.sample.app.navigation.Navigator
import uno.lux.sample.app.navigation.Screen
import uno.lux.sample.app.util.AppError
import uno.lux.sample.app.util.ignoreErrors
import uno.lux.sample.app.util.launchCatching
import uno.lux.sample.app.util.launchIfIdle
import uno.lux.sample.comment.data.CommentRepository
import uno.lux.sample.comment.data.domain.Comment
import uno.lux.sample.comment.data.domain.CommentId
import uno.lux.sample.common.data.ReportReason
import uno.lux.sample.common.data.network.toAppError
import uno.lux.sample.common.ui.FailedAction
import uno.lux.sample.common.ui.launchReporting
import uno.lux.sample.post.data.PostRepository
import uno.lux.sample.post.data.domain.PostId
import uno.lux.sample.post.ui.PostDetailUiState.Content
import uno.lux.sample.user.data.UserRepository
import uno.lux.sample.user.data.domain.User

/**
 * Holds the state for a single post's detail view.
 *
 * The state is **one value the ViewModel owns and edits**, not a projection assembled from a flow
 * per moving part. Everything this page discovers for itself — the thread, the send states, the
 * announcement — is a field of [PostDetailUiState], written through [mutateState]. What comes
 * from outside enters through exactly one collector, [observeStores], because the post really
 * does belong to somebody else: [PostRepository] holds it in a store shared with the feed and
 * every profile, so a like toggled underneath, or a delete performed on another screen, has to
 * reach this page without it asking. That is the one thing a plain field could not do, and it is
 * the only reason a subscription remains.
 *
 * Comments, by contrast, are this ViewModel's own: a first page is loaded on creation,
 * [loadMoreComments] appends the rest a window at a time as the reader scrolls, and the lot is
 * discarded when the ViewModel is cleared. This avoids the cross-post keying and memory-retention
 * problems a shared comment store would introduce.
 *
 * The entity store normally already holds the post — this page is reached from a feed or profile
 * that fetched it. It does not when the page is restored after **process death**, where the back
 * stack comes back but every in-memory store starts empty, so [loadPost] fetches the post it was
 * given the ID for. That fetch is why an absent post can't simply mean [Content.NotFound]: "not
 * asked yet", "the server says it's gone" and "the request failed" are three different screens,
 * and [PostFetch] is what tells them apart.
 *
 * Intent arrives as one [PostDetailUiEvent] through [onEvent], which the screen reaches through
 * [PostDetailUiState.eventSink]. [postId] is a runtime argument wired through [Factory] /
 * assisted injection, so every opened post gets its own ViewModel, scoped to its back-stack entry.
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
    private sealed interface PostFetch {
        data object Pending : PostFetch

        data object Missing : PostFetch

        data class Failed(
            val error: AppError,
        ) : PostFetch
    }

    /**
     * A plain field rather than a flow: nothing observes it, [content] reads it, and every write
     * goes through [setFetch], which recomputes the state in the same breath. Declared above
     * [_uiState] because that state's initial value already reads it.
     */
    private var fetch: PostFetch = PostFetch.Pending

    /**
     * The page, as one value this ViewModel owns and edits. Seeded from [content] rather than
     * from [Content.Loading], so a page opened the ordinary way — from a feed or profile that
     * already fetched the post — is [Content.Loaded] before the first frame, with no spinner
     * flashed for a post that was never missing.
     *
     * Every write goes through [update][kotlinx.coroutines.flow.update] and touches only the
     * fields it owns, which is the same discipline `PostRepository.updateEntity` exists for:
     * reading the state into a local, suspending, then writing that local back would overwrite
     * whatever landed in between. `update` retries its lambda under contention, so what is passed
     * must be side-effect-free and safe to run more than once — a `copy`, or a `copy` over
     * something re-read as [updateContent] re-reads [content].
     */
    private val _uiState = MutableStateFlow(
        PostDetailUiState(
            content = content(),
            composerUser = currentUser,
            eventSink = ::onEvent,
        ),
    )

    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var reportJob: Job? = null

    init {
        observeStores()
        retry()
    }

    // ── Intent ────────────────────────────────────────────────────────────────

    /**
     * The one way in. Each branch names its work rather than doing it, so the `when` reads as the
     * page's vocabulary — the exceptions are the three signals the screen spends, which are a
     * single field write each and would only be obscured by a method to hold them.
     */
    fun onEvent(event: PostDetailUiEvent) {
        when (event) {
            PostDetailUiEvent.GoBack -> navigator.goBack()
            is PostDetailUiEvent.OpenProfile -> navigator.goTo(Screen.Profile(event.userId))
            is PostDetailUiEvent.OpenVideo -> navigator.goTo(Screen.FullscreenVideo(event.video))
            is PostDetailUiEvent.OpenAlbum -> navigator.goTo(
                Screen.AlbumViewer(event.imageUrls, event.initialIndex),
            )

            PostDetailUiEvent.ToggleLike -> toggleLike()
            PostDetailUiEvent.ToggleBookmark -> toggleBookmark()
            PostDetailUiEvent.Delete -> delete()
            is PostDetailUiEvent.Report -> report(event.reason, event.details)
            PostDetailUiEvent.CloseReport -> dropReport(::reportJob, ::setReportSend)
            PostDetailUiEvent.Retry -> retry()

            is PostDetailUiEvent.AddComment -> addComment(event.text)
            is PostDetailUiEvent.ToggleCommentLike -> toggleCommentLike(event.commentId)
            PostDetailUiEvent.LoadMoreComments -> loadMoreComments()
            PostDetailUiEvent.RetryComments -> retryComments()

            PostDetailUiEvent.FailedActionShown -> _uiState.update { it.copy(failedAction = null) }
            PostDetailUiEvent.CommentSent -> setCommentSend(CommentSendState.IDLE)
            PostDetailUiEvent.ScrolledToComment -> mutateThread { it.copy(scrollTo = null) }
        }
    }

    // ── The state ─────────────────────────────────────────────────────────────

    /** Edits the thread in place, leaving the rest of the page as it was. */
    private fun mutateThread(mutate: (CommentThread) -> CommentThread) = _uiState.update {
        it.copy(thread = mutate(it.thread))
    }

    private fun setCommentSend(state: CommentSendState) = _uiState.update {
        it.copy(commentSend = state)
    }

    /** Passed to [launchReport] and [dropReport] by reference, which is why it is a function. */
    private fun setReportSend(state: ReportSendState) = _uiState.update {
        it.copy(reportSend = state)
    }

    /** Passed to [launchReporting] by reference, which is why it is a function. */
    private fun setFailedAction(action: FailedAction) = _uiState.update {
        it.copy(failedAction = action)
    }

    // ── The post ──────────────────────────────────────────────────────────────

    /**
     * The single subscription this page keeps. The stores are the one input it does not own, and
     * all three move for reasons that have nothing to do with this post, so the collector only
     * asks [content] to work out what this page shows now. An answer equal to the last one leaves
     * the state untouched — [MutableStateFlow] conflates by equality — which is the dedup the
     * old `distinctUntilChanged` chain had to be spelled out for.
     *
     * It runs for the ViewModel's whole life rather than only while the screen is watching. That
     * is the price of the state being a value rather than something shared `WhileSubscribed`, and
     * it is worth paying: two map lookups on an emission this page does not care about, against
     * [uiState] being current the moment the ViewModel exists — which is what lets a restored page
     * draw its post without a frame of spinner, and what the tests read directly.
     */
    private fun observeStores() {
        viewModelScope.launch {
            combine(
                postRepository.entities,
                userRepository.users,
                postRepository.deletedIds,
            ) { _, _, _ -> }.collect { updateContent() }
        }
    }

    private fun updateContent() = _uiState.update { it.copy(content = content()) }

    /**
     * What the page shows, given the stores and how the cold-start fetch went. Reads the stores
     * through `.value` rather than from an emission, which can only ever be more current.
     *
     * A resolved post outranks everything, deletion included: [PostRepository.delete] drops the
     * entity as it records the ID, so the two cannot disagree, and were the order reversed a
     * refresh landing mid-delete would win against the answer.
     */
    private fun content(): Content {
        val post = postRepository.entities.value[postId]
        val author = post?.let { userRepository.users.value[it.authorId] }
        if (post != null && author != null) {
            return Content.Loaded(
                post = post,
                author = author,
                isOwn = post.authorId == currentUser.id,
            )
        }

        // Told by the store that the post is gone, whichever screen deleted it. Without this the
        // emptied store would read as "not loaded yet" and leave the page spinning on a fetch
        // nobody is going to make.
        if (postId in postRepository.deletedIds.value) return Content.NotFound

        return when (val fetch = fetch) {
            PostFetch.Pending -> Content.Loading
            PostFetch.Missing -> Content.NotFound
            is PostFetch.Failed -> Content.Error(fetch.error)
        }
    }

    private fun setFetch(outcome: PostFetch) {
        fetch = outcome
        updateContent()
    }

    /** Reloads everything the page failed to get — the post itself included. */
    private fun retry() = launchIfIdle(::loadJob) {
        coroutineScope {
            launch { loadPost() }
            launch { loadComments() }
        }
    }

    /**
     * Fetches the post unless the stores already hold it with its author, which they do whenever
     * this page was opened from a feed or profile. The fetch is for the cold start: a detail page
     * restored after process death, where this ViewModel is the first thing to ask for the post.
     */
    private suspend fun loadPost() {
        if (_uiState.value.content is Content.Loaded) return

        setFetch(PostFetch.Pending)
        ignoreErrors(onError = { e -> setFetch(PostFetch.Failed(e.toAppError())) }) {
            // A 404 is the server saying the post is gone — an answer, not a failure to retry.
            if (postRepository.load(postId) == null) setFetch(PostFetch.Missing)
        }
    }

    private fun toggleLike() = launchCatching {
        postRepository.toggleLike(postId)
    }

    private fun toggleBookmark() = launchCatching {
        postRepository.toggleBookmark(postId)
    }

    /**
     * Deletes the post and pops this screen. Backing out is part of the outcome, not the caller's
     * follow-up: once the entity is gone the detail view has nothing left to show, and the feed or
     * profile underneath has already dropped the post through the shared entity store.
     */
    private fun delete() = launchReporting(FailedAction.DELETE_POST, ::setFailedAction) {
        // The store marks the deletion, and [content] reads NotFound from it — the same path a
        // deletion performed on any other screen takes. A failed delete throws before the pop, so
        // the page stays put and announces the failure itself.
        postRepository.delete(postId)
        navigator.goBack()
    }

    /**
     * Reports the post. Unlike [delete] the page stays where it is: a reported post is still a
     * post, and the reporter is still reading it — and so is the dialog, which is why the outcome
     * goes to [PostDetailUiState.reportSend] rather than to a snackbar the dialog would cover.
     */
    private fun report(reason: ReportReason, details: String) =
        launchReport(::reportJob, ::setReportSend) {
            postRepository.report(postId, reason, details)
        }

    // ── The thread ────────────────────────────────────────────────────────────

    private fun retryComments() {
        viewModelScope.launch { loadComments() }
    }

    /**
     * Loads the thread's first page, replacing whatever was there — a retry after a failure, and
     * the reload [retry] performs, both start the thread over rather than resuming a window whose
     * cursor may describe a page that has since moved.
     */
    private suspend fun loadComments() {
        mutateThread { it.copy(isLoading = true, error = null) }

        try {
            ignoreErrors(onError = { e -> mutateThread { it.copy(error = e.toAppError()) } }) {
                val page = commentRepository.loadComments(postId, cursor = null)
                mutateThread {
                    it.copy(
                        comments = page.comments,
                        nextCursor = page.nextCursor,
                        endReached = !page.hasMore,
                        // This page starts the thread over, so a scroll owed to a comment in the
                        // window it replaces has nothing left to aim at.
                        scrollTo = null,
                    )
                }
            }
        } finally {
            mutateThread { it.copy(isLoading = false) }
        }
    }

    /**
     * Appends the page after the one loaded last. A thread of any length is read a window at a
     * time, so a post with hundreds of comments costs the same first request as one with three.
     *
     * The cursor is the whole guard: it is null before the first page lands and again once the
     * server says that page was the last, so "there is nothing to ask for" and "we have it all"
     * need no flag of their own. [loadMoreJob] covers the third case — the screen asking again
     * while a page is still on the wire.
     */
    private fun loadMoreComments() = launchIfIdle(::loadMoreJob) {
        val cursor = _uiState.value.thread.nextCursor ?: return@launchIfIdle

        ignoreErrors {
            val page = commentRepository.loadComments(postId, cursor)
            mutateThread { thread ->
                // A reload that landed while this page was on the wire started the thread over,
                // and this window no longer follows what is on screen. Dropping it is the same
                // move [toggleCommentLike] makes on an answer a newer tap has passed.
                if (thread.nextCursor != cursor) {
                    thread
                } else {
                    thread.copy(
                        comments = thread.comments + page.comments,
                        nextCursor = page.nextCursor,
                        endReached = !page.hasMore,
                    )
                }
            }
        }
    }

    /**
     * Flips the viewer's like on one comment in the thread, moving it *before* the request goes
     * out and reconciling with the server's answer when it lands.
     *
     * The same shape [PostRepository.toggleLike] has, and deliberately not shared with it: a
     * comment's like lives in the thread this ViewModel owns rather than in an entity store, so
     * the two differ in the one thing — where the item is — that the code is about. What they do
     * share is the reasoning, and [updateComment] is this side's [PostRepository] `updateEntity`:
     * every write re-reads the comment and touches only the like fields, so a reload landing
     * mid-flight is not overwritten by a copy of the comment as it was before the request.
     */
    private fun toggleCommentLike(commentId: CommentId) = launchCatching {
        val thread = _uiState.value.thread
        val before = thread.comments.find { it.id == commentId } ?: return@launchCatching
        val liked = !before.isLiked
        // A later tap has already moved past this request, so its answer is the one to trust.
        val stillOurs = { comment: Comment -> comment.isLiked == liked }

        updateComment(commentId) {
            it.copy(isLiked = liked, likeCount = it.likeCount + if (liked) 1 else -1)
        }

        try {
            val confirmed = commentRepository.setLike(postId, commentId, liked)
            updateComment(commentId, stillOurs) {
                it.copy(isLiked = confirmed.isLiked, likeCount = confirmed.likeCount)
            }
        } catch (e: CancellationException) {
            // The screen went away, not the request: it is on the wire and the server most
            // likely took it, so the optimistic value is the better guess to leave behind.
            throw e
        } catch (e: Exception) {
            updateComment(commentId, stillOurs) {
                it.copy(isLiked = before.isLiked, likeCount = before.likeCount)
            }
            throw e
        }
    }

    /**
     * Applies [edit] to [commentId] where it sits in the thread, if it is still there and
     * [stillOurs] accepts what it currently says. A comment the reload replaced, or one a newer
     * tap has moved past, is left alone.
     */
    private fun updateComment(
        commentId: CommentId,
        stillOurs: (Comment) -> Boolean = { true },
        edit: (Comment) -> Comment,
    ) = mutateThread { thread ->
        thread.copy(
            comments = thread.comments.map { comment ->
                if (comment.id == commentId && stillOurs(comment)) edit(comment) else comment
            },
        )
    }

    /**
     * Posts a comment, prepending it to the thread this page owns and telling the entity store the
     * post gained one. The two halves land in different places on purpose: the comment is this
     * ViewModel's, but the count under the post belongs to the shared entity, so it is what the
     * feed card and the profile behind this page redraw from.
     *
     * The comment is also named as the one to scroll to. The thread sits below the whole post, so
     * a reader who commented from the bottom of a long page would otherwise never see what they
     * sent. Only a comment the server took is named, so a failed send moves nothing.
     *
     * Not [launchCatching], because nothing about a comment is optimistic: the composer holds the
     * text until [CommentSendState.SENT] says the server has it, so a failure that logged and
     * discarded would leave an emptied box and no comment. The state machine is the whole
     * contract — [CommentSendState.SENDING] disables the composer, so this cannot run twice over,
     * and a failure returns to [CommentSendState.IDLE] with the text still there to send again.
     */
    private fun addComment(text: String) =
        launchReporting(FailedAction.SEND_COMMENT, ::setFailedAction) {
            setCommentSend(CommentSendState.SENDING)

            try {
                val comment = commentRepository.addComment(postId, text)
                mutateThread {
                    it.copy(comments = listOf(comment) + it.comments, scrollTo = comment.id)
                }
                postRepository.commentAdded(postId)
                setCommentSend(CommentSendState.SENT)
            } catch (e: Exception) {
                // Back to idle *before* rethrowing, so the composer is editable again by the time
                // [launchReporting] announces the failure.
                setCommentSend(CommentSendState.IDLE)
                throw e
            }
        }
}
