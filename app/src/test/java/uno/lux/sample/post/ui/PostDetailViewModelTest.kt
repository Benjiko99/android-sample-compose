package uno.lux.sample.post.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.app.navigation.Navigator
import uno.lux.sample.app.navigation.Screen
import uno.lux.sample.app.util.AppError
import uno.lux.sample.comment.data.CommentDataSource
import uno.lux.sample.comment.data.CommentPage
import uno.lux.sample.comment.data.CommentRepository
import uno.lux.sample.comment.data.FakeCommentDataSource
import uno.lux.sample.comment.data.domain.Comment
import uno.lux.sample.comment.data.domain.CommentId
import uno.lux.sample.common.data.LikeState
import uno.lux.sample.common.data.ReportReason
import uno.lux.sample.common.ui.FailedAction
import uno.lux.sample.post.data.FakePostDataSource
import uno.lux.sample.post.data.PostRepository
import uno.lux.sample.post.data.domain.Post
import uno.lux.sample.post.data.domain.PostId
import uno.lux.sample.post.data.domain.PostWithAuthor
import uno.lux.sample.testing.ViewModelTest
import uno.lux.sample.testing.backStackOf
import uno.lux.sample.testing.screens
import uno.lux.sample.testing.testPostUrl
import uno.lux.sample.user.data.FakeUserDataSource
import uno.lux.sample.user.data.UserRepository
import uno.lux.sample.user.data.domain.User
import java.net.UnknownHostException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailViewModelTest : ViewModelTest() {

    private val currentUser = User(id = "me", nickname = "Me", handle = "@me")
    private val otherUser = User(id = "u2", nickname = "Other", handle = "@other")

    private val post = Post(
        id = "p1",
        url = testPostUrl("p1"),
        authorId = "u2",
        title = "The Title",
        body = "The body.",
        createdAt = Instant.EPOCH,
        likeCount = 3,
        commentCount = 1,
    )

    private val seedComment = Comment(
        id = "c1",
        author = otherUser,
        createdAt = Instant.EPOCH,
        text = "Nice post",
        likeCount = 2,
    )

    // The real Navigator drives a plain list, so navigation tests assert on the stack directly.
    private val backStack = backStackOf(Screen.Shell, Screen.PostDetail("p1"))
    private val navigator = Navigator().apply { attach(backStack) }

    /** The entity store the last [viewModel] was built on, for driving it from outside. */
    private lateinit var postRepository: PostRepository

    private fun viewModel(
        postId: String = "p1",
        posts: List<Post> = listOf(post),
        users: List<User> = listOf(currentUser, otherUser),
        comments: Map<String, List<Comment>> = mapOf("p1" to listOf(seedComment)),
        commentDataSource: CommentDataSource = FakeCommentDataSource(currentUser, comments),
        postDataSource: FakePostDataSource = FakePostDataSource(),
    ): PostDetailViewModel {
        val userRepo = UserRepository(FakeUserDataSource())
        userRepo.ingest(users)
        val postRepo = PostRepository(postDataSource, userRepo)
        postRepo.ingest(posts)
        postRepository = postRepo
        return PostDetailViewModel(
            postRepository = postRepo,
            commentRepository = CommentRepository(commentDataSource),
            userRepository = userRepo,
            navigator = navigator,
            currentUser = currentUser,
            postId = postId,
        )
    }

    /** A comment source on p1, typed as the fake so a test can reach its like bookkeeping. */
    private fun commentSource(thread: List<Comment> = listOf(seedComment)) =
        FakeCommentDataSource(currentUser, mapOf("p1" to thread))

    /** A thread of [size] comments, ids `c1`..`cN` in the order the server would send them. */
    private fun thread(size: Int) = List(size) { index ->
        seedComment.copy(id = "c${index + 1}", text = "Comment ${index + 1}")
    }

    /** A comment source on p1 that hands its thread out [pageSize] comments at a time. */
    private fun pagedSource(thread: List<Comment>, pageSize: Int) =
        FakeCommentDataSource(currentUser, mapOf("p1" to thread), pageSize = pageSize)

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    fun `a post by someone else is not marked own`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertFalse((viewModel.uiState.value as PostDetailUiState.Loaded).isOwn)
    }

    @Test
    fun `a post by the signed-in user is marked own`() = runTest {
        val viewModel = viewModel(posts = listOf(post.copy(authorId = "me")))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertTrue((viewModel.uiState.value as PostDetailUiState.Loaded).isOwn)
    }

    @Test
    fun `onDelete removes the post and pops the detail page`() = runTest {
        val viewModel = viewModel(posts = listOf(post.copy(authorId = "me")))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onDelete()

        assertEquals(PostDetailUiState.NotFound, viewModel.uiState.value)
        assertEquals(listOf(Screen.Shell), backStack.screens())
    }

    // A refused delete must not pop the page: the post is still there, and leaving would make
    // the failure read as success. The announcement is the only thing the tap produces.
    @Test
    fun `a failed delete keeps the page open and names the action for the screen`() = runTest {
        val dataSource = FakePostDataSource().apply { deleteError = UnknownHostException("offline") }
        val viewModel = viewModel(
            posts = listOf(post.copy(authorId = "me")),
            postDataSource = dataSource,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onDelete()

        assertTrue(viewModel.uiState.value is PostDetailUiState.Loaded)
        assertEquals(listOf(Screen.Shell, Screen.PostDetail("p1")), backStack.screens())
        assertEquals(FailedAction.DELETE_POST, viewModel.failedAction.value)
    }

    @Test
    fun `a failed report names the action for the screen`() = runTest {
        val dataSource = FakePostDataSource().apply { reportError = UnknownHostException("offline") }
        val viewModel = viewModel(postDataSource = dataSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onReport(ReportReason.HARASSMENT, "")

        assertEquals(FailedAction.REPORT_POST, viewModel.failedAction.value)
    }

    // Announced once and then spent, the same lifetime the feed gives its refresh error: what is
    // left in the state is what a rotation would replay.
    @Test
    fun `onFailedActionShown clears the announcement`() = runTest {
        val dataSource = FakePostDataSource().apply { reportError = UnknownHostException("offline") }
        val viewModel = viewModel(postDataSource = dataSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        viewModel.onReport(ReportReason.HARASSMENT, "")

        viewModel.onFailedActionShown()

        assertNull(viewModel.failedAction.value)
    }

    // This page can sit *under* the screen that deletes its post — open a post, visit the
    // author's profile, delete it there, come back. Absence from the store is ambiguous
    // (never loaded? gone?), so the deletion has to reach this page as an answer, not as an
    // emptied store that reads as "not loaded yet" and spins forever.
    @Test
    fun `a post deleted from another screen reads as gone, not loading`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertTrue(vm.uiState.value is PostDetailUiState.Loaded)

        postRepository.delete("p1")

        assertEquals(PostDetailUiState.NotFound, vm.uiState.value)
    }

    // ── report ────────────────────────────────────────────────────────────────

    // A report is about the post, not on it: unlike a deletion it changes nothing the page
    // shows, and leaves the reporter reading what they reported.
    @Test
    fun `onReport reports the post and keeps the page open`() = runTest {
        val dataSource = FakePostDataSource()
        val viewModel = viewModel(postDataSource = dataSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onReport(ReportReason.HARASSMENT, "Second time this week")

        assertEquals(
            listOf(FakePostDataSource.Report("p1", ReportReason.HARASSMENT, "Second time this week")),
            dataSource.reports,
        )
        assertTrue(viewModel.uiState.value is PostDetailUiState.Loaded)
        assertEquals(listOf(Screen.Shell, Screen.PostDetail("p1")), backStack.screens())
    }

    // ── navigation ────────────────────────────────────────────────────────────

    @Test
    fun `goBack pops the detail page`() {
        viewModel().goBack()

        assertEquals(listOf(Screen.Shell), backStack.screens())
    }

    @Test
    fun `openProfile pushes the author's profile page`() {
        viewModel().openProfile("u2")

        assertEquals(Screen.Profile("u2"), backStack.last().screen)
    }

    @Test
    fun `openAlbum pushes the album viewer at the tapped image`() {
        val images = listOf("https://example.test/1.jpg")

        viewModel().openAlbum(images, initialIndex = 0)

        assertEquals(Screen.AlbumViewer(images, initialIndex = 0), backStack.last().screen)
    }

    // ── uiState ───────────────────────────────────────────────────────────────

    @Test
    fun `uiState is Loading until subscribed`() {
        assertEquals(PostDetailUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `uiState is Loaded with post and comments once subscribed`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertEquals(post, state.post)
        assertEquals(otherUser, state.author)
        assertEquals(listOf(seedComment), state.comments)
        assertFalse(state.commentsLoading)
    }

    @Test
    fun `comments are marked loading until they resolve`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        val vm = viewModel(commentDataSource = GatedCommentDataSource(gate, listOf(seedComment)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        assertTrue((vm.uiState.value as PostDetailUiState.Loaded).commentsLoading)

        gate.complete(Unit)
        advanceUntilIdle()

        val loaded = vm.uiState.value as PostDetailUiState.Loaded
        assertFalse(loaded.commentsLoading)
        assertEquals(listOf(seedComment), loaded.comments)
    }

    // ── cold start (restored after process death) ──────────────────────────────
    //
    // The stores are empty when the app is restarted straight onto this page, so the ViewModel
    // has to fetch the post its back-stack key names. Whether that fetch is still running, came
    // back empty, or failed outright are three different screens.

    @Test
    fun `a page restored with empty stores fetches the post it was given the id for`() = runTest {
        val dataSource = FakePostDataSource().apply {
            fetchable["p1"] = PostWithAuthor(post, otherUser)
        }
        val vm = viewModel(posts = emptyList(), users = emptyList(), postDataSource = dataSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertEquals(post, state.post)
        // The author rides along with the post, so the header resolves without a second request.
        assertEquals(otherUser, state.author)
    }

    @Test
    fun `a post the stores already hold is not re-fetched`() = runTest {
        val dataSource = FakePostDataSource()
        val vm = viewModel(postDataSource = dataSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(emptyList<PostId>(), dataSource.fetchedPostIds)
        assertTrue(vm.uiState.value is PostDetailUiState.Loaded)
    }

    @Test
    fun `uiState stays Loading while the fetch is in flight`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(posts = emptyList(), users = emptyList())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(PostDetailUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `uiState is NotFound when the server no longer has the post`() = runTest {
        val vm = viewModel(postId = "missing")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(PostDetailUiState.NotFound, vm.uiState.value)
    }

    // A failed request says nothing about whether the post exists, so it must not read as "gone".
    @Test
    fun `uiState is a retryable Error when the fetch fails`() = runTest {
        val dataSource = FakePostDataSource().apply { fetchError = UnknownHostException("offline") }
        val vm = viewModel(posts = emptyList(), users = emptyList(), postDataSource = dataSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(PostDetailUiState.Error(AppError.NoConnection), vm.uiState.value)
    }

    @Test
    fun `retry after a failed fetch loads the post`() = runTest {
        val dataSource = FakePostDataSource().apply { fetchError = UnknownHostException("offline") }
        val vm = viewModel(posts = emptyList(), users = emptyList(), postDataSource = dataSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        dataSource.fetchError = null
        dataSource.fetchable["p1"] = PostWithAuthor(post, otherUser)
        vm.retry()
        advanceUntilIdle()

        assertEquals(post, (vm.uiState.value as PostDetailUiState.Loaded).post)
    }

    // ── composerUser ──────────────────────────────────────────────────────────

    @Test
    fun `composerUser is the current user`() {
        assertEquals(currentUser, viewModel().composerUser)
    }

    // ── like / bookmark ───────────────────────────────────────────────────────

    @Test
    fun `onToggleLike likes the post through the shared entity store`() = runTest {
        // The count in the answer is the server's, so the fake is told what it started from.
        val vm = viewModel(
            postDataSource = FakePostDataSource().apply { likeCounts["p1"] = post.likeCount },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onToggleLike()

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertTrue(state.post.isLiked)
        assertEquals(4, state.post.likeCount)
    }

    @Test
    fun `onToggleBookmark bookmarks the post through the shared entity store`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onToggleBookmark()

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertTrue(state.post.isBookmarked)
    }

    // ── comment interactions ──────────────────────────────────────────────────

    @Test
    fun `addComment prepends a new comment to the thread`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.addComment("Hello!")

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertEquals(2, state.comments.size)
        val newest = state.comments.first()
        assertEquals("Hello!", newest.text)
        assertEquals(currentUser, newest.author)
    }

    // The comment lands in this ViewModel's own list, but the count under the post is a field on
    // the entity — so it has to go through the store, or the header here, the feed card and the
    // profile all keep showing the number the post had before the user commented on it.
    @Test
    fun `addComment bumps the post's comment count in the shared store`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.addComment("Hello!")

        assertEquals(2, (vm.uiState.value as PostDetailUiState.Loaded).post.commentCount)
        assertEquals(2, postRepository.entities.value["p1"]!!.commentCount)
    }

    // The thread and the count arrive from two different flows into one combine, and the new
    // comment appearing while the count under the post stays put is what a stale build looks
    // like. Driven on a real dispatcher rather than an unconfined one, so an emission the combine
    // dropped would settle wrong instead of being papered over.
    @Test
    fun `the thread and the count settle together on the same state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.addComment("Hello!")
        advanceUntilIdle()

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertEquals("the thread carries the new comment", 2, state.comments.size)
        assertEquals("the header and action row count the post", 2, state.post.commentCount)
    }

    // The count follows the comment: a post that never landed must not inflate it.
    @Test
    fun `a failed addComment leaves the comment count alone`() = runTest {
        val commentDataSource = FakeCommentDataSource(currentUser, mapOf("p1" to listOf(seedComment)))
            .apply { addError = UnknownHostException("offline") }
        val vm = viewModel(commentDataSource = commentDataSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.addComment("Hello!")

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertEquals(1, state.post.commentCount)
        assertEquals(listOf(seedComment), state.comments)
    }

    // The composer holds the text until the server has the comment, so SENT is the signal that
    // says it may finally let go of it.
    @Test
    fun `a comment the server takes ends in SENT`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.addComment("Hello!")

        assertEquals(
            CommentSendState.SENT,
            (vm.uiState.value as PostDetailUiState.Loaded).commentSend,
        )
    }

    // The field and the send button are disabled off this state. Without it a second tap would
    // post the comment twice, since nothing else stops one.
    @Test
    fun `the composer reads as sending while the comment is on the wire`() = runTest {
        val answered = CompletableDeferred<Unit>()
        val source = commentSource().apply { whileAdding = { answered.await() } }
        val vm = viewModel(commentDataSource = source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.addComment("Hello!")
        advanceUntilIdle()

        assertEquals(
            CommentSendState.SENDING,
            (vm.uiState.value as PostDetailUiState.Loaded).commentSend,
        )

        answered.complete(Unit)
        advanceUntilIdle()

        assertEquals(
            CommentSendState.SENT,
            (vm.uiState.value as PostDetailUiState.Loaded).commentSend,
        )
    }

    // The whole point of not clearing up front: a refused comment leaves the composer editable,
    // with what the user typed still in it, and says so.
    @Test
    fun `a failed send returns the composer to idle and names the action`() = runTest {
        val source = commentSource().apply { addError = UnknownHostException("offline") }
        val vm = viewModel(commentDataSource = source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.addComment("Hello!")

        assertEquals(
            CommentSendState.IDLE,
            (vm.uiState.value as PostDetailUiState.Loaded).commentSend,
        )
        assertEquals(FailedAction.SEND_COMMENT, vm.failedAction.value)
    }

    // Spent once the composer has given the text up, so a rotation cannot clear a box the user
    // has already started refilling.
    @Test
    fun `onCommentSent spends the send signal`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.addComment("Hello!")

        vm.onCommentSent()

        assertEquals(
            CommentSendState.IDLE,
            (vm.uiState.value as PostDetailUiState.Loaded).commentSend,
        )
    }

    // The thread hangs below the whole post, so a comment sent from a long page lands off screen
    // unless the screen is told to go to it.
    @Test
    fun `addComment names the new comment as the one to scroll to`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.addComment("Hello!")

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertEquals(state.comments.first().id, state.scrollToComment)
    }

    // Spent once acted on: a configuration change must not yank the list back a second time.
    @Test
    fun `onScrolledToComment spends the scroll signal`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.addComment("Hello!")

        vm.onScrolledToComment()

        assertNull((vm.uiState.value as PostDetailUiState.Loaded).scrollToComment)
    }

    // Nothing was posted, so there is nothing to go to.
    @Test
    fun `a failed addComment names nothing to scroll to`() = runTest {
        val commentDataSource = commentSource().apply { addError = UnknownHostException("offline") }
        val vm = viewModel(commentDataSource = commentDataSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.addComment("Hello!")

        assertNull((vm.uiState.value as PostDetailUiState.Loaded).scrollToComment)
    }

    // A reload starts the thread over, so a scroll owed into the window it replaced has nothing
    // left to aim at — and the screen would otherwise jump to whatever now sits at that index.
    @Test
    fun `retrying comments drops a pending scroll`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.addComment("Hello!")

        vm.retryComments()

        assertNull((vm.uiState.value as PostDetailUiState.Loaded).scrollToComment)
    }

    @Test
    fun `onToggleCommentLike likes a comment in the thread`() = runTest {
        // The count in the answer is the server's, so the fake is told what it started from.
        val vm = viewModel(commentDataSource = commentSource().apply { likeCounts["c1"] = 2 })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onToggleCommentLike("c1")

        val state = vm.uiState.value as PostDetailUiState.Loaded
        val liked = state.comments.single()
        assertTrue(liked.isLiked)
        assertEquals(3, liked.likeCount)
    }

    @Test
    fun `onToggleCommentLike asks for the state opposite the comment's own`() = runTest {
        val source = commentSource(listOf(seedComment.copy(isLiked = true)))
        val vm = viewModel(commentDataSource = source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onToggleCommentLike("c1")

        assertEquals(listOf("c1" to false), source.likeRequests)
    }

    // The heart under a comment fills on the tap for the same reason the post's does — waiting out
    // a round trip reads as broken. The request is held open on [answered] so the in-flight state
    // can be read, and the fake's server is seeded to disagree with the count the thread was
    // loaded with, which is what tells the optimistic value (3) from the confirmed one (11).
    @Test
    fun `onToggleCommentLike fills the heart before the request answers`() = runTest {
        val answered = CompletableDeferred<Unit>()
        val source = commentSource().apply {
            likeCounts["c1"] = 10
            whileInFlight = { answered.await() }
        }
        val vm = viewModel(commentDataSource = source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onToggleCommentLike("c1")
        advanceUntilIdle()

        val inFlight = (vm.uiState.value as PostDetailUiState.Loaded).comments.single()
        assertTrue(inFlight.isLiked)
        assertEquals(3, inFlight.likeCount)

        answered.complete(Unit)
        advanceUntilIdle()

        // And once it lands, the server's count replaces the guess.
        assertEquals(11, (vm.uiState.value as PostDetailUiState.Loaded).comments.single().likeCount)
    }

    @Test
    fun `a failed comment like puts the heart back`() = runTest {
        val source = commentSource().apply {
            likeCounts["c1"] = 2
            setLikeError = UnknownHostException("offline")
        }
        val vm = viewModel(commentDataSource = source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onToggleCommentLike("c1")

        val comment = (vm.uiState.value as PostDetailUiState.Loaded).comments.single()
        assertFalse(comment.isLiked)
        assertEquals(2, comment.likeCount)
    }

    // A comment like writes only the two fields it owns, so a thread reloaded while the request
    // was in flight keeps the text it brought back rather than the copy the tap was made against.
    @Test
    fun `a comment like answer leaves the rest of the comment alone`() = runTest {
        val source = commentSource().apply { likeCounts["c1"] = 2 }
        val vm = viewModel(commentDataSource = source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        source.whileInFlight = {
            source.comments = mapOf("p1" to listOf(seedComment.copy(text = "Edited", isLiked = true)))
            vm.retryComments()
        }

        vm.onToggleCommentLike("c1")

        val comment = (vm.uiState.value as PostDetailUiState.Loaded).comments.single()
        assertEquals("Edited", comment.text)
        assertTrue(comment.isLiked)
    }

    // Two taps in flight: the second is what the user last asked for, so the first request's
    // answer — which describes a state nobody is waiting for — must not overwrite it.
    @Test
    fun `a comment like answer that a newer tap has moved past is dropped`() = runTest {
        val source = commentSource().apply { likeCounts["c1"] = 2 }
        val vm = viewModel(commentDataSource = source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        source.whileInFlight = {
            // Only the first request re-enters; the second must not recurse.
            source.whileInFlight = null
            vm.onToggleCommentLike("c1")
        }

        vm.onToggleCommentLike("c1")

        assertEquals(listOf("c1" to true, "c1" to false), source.likeRequests)
        assertFalse((vm.uiState.value as PostDetailUiState.Loaded).comments.single().isLiked)
    }

    // ── comment pagination ────────────────────────────────────────────────────
    //
    // A thread arrives a window at a time, so opening a post with hundreds of comments costs the
    // same first request as opening one with three.

    @Test
    fun `only the first page of the thread is loaded up front`() = runTest {
        val full = thread(5)
        val source = pagedSource(full, pageSize = 2)
        val vm = viewModel(commentDataSource = source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertEquals(full.take(2), state.comments)
        assertFalse(state.commentsEndReached)
        assertEquals(listOf("p1" to null), source.loadRequests)
    }

    @Test
    fun `loadMoreComments appends the page after the one loaded last`() = runTest {
        val full = thread(5)
        val vm = viewModel(commentDataSource = pagedSource(full, pageSize = 2))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.loadMoreComments()

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertEquals(full.take(4), state.comments)
        assertFalse(state.commentsEndReached)
    }

    // The end of the thread is the server's to declare, and the screen stops asking on it — the
    // spinner under the last comment has to become nothing rather than spin forever.
    @Test
    fun `the thread ends when the last page lands, and nothing more is asked for`() = runTest {
        val full = thread(4)
        val source = pagedSource(full, pageSize = 2)
        val vm = viewModel(commentDataSource = source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.loadMoreComments()
        vm.loadMoreComments()

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertEquals(full, state.comments)
        assertTrue(state.commentsEndReached)
        assertEquals(listOf("p1" to null, "p1" to "c2"), source.loadRequests)
    }

    // The list is near its end while the first page is still on the wire, so the screen asks
    // early. There is no cursor to ask with yet, and a request without one would re-fetch the
    // page already coming.
    @Test
    fun `loadMoreComments waits for the first page to hand back a cursor`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val source = pagedSource(thread(5), pageSize = 2).apply { whileLoading = { gate.await() } }
        val vm = viewModel(commentDataSource = source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.loadMoreComments()

        assertEquals(listOf("p1" to null), source.loadRequests)

        gate.complete(Unit)
    }

    // A page that failed leaves the thread as it was: the comments already read stay on screen.
    @Test
    fun `a failed page leaves the loaded thread alone`() = runTest {
        val full = thread(5)
        val source = pagedSource(full, pageSize = 2)
        val vm = viewModel(commentDataSource = source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        source.loadError = UnknownHostException("offline")
        vm.loadMoreComments()

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertEquals(full.take(2), state.comments)
        assertFalse(state.commentsEndReached)
    }

    // A retry is a fresh thread, not a resumed one: the cursor it was holding describes a window
    // into a thread that may have moved since.
    @Test
    fun `retryComments starts the thread over from the first page`() = runTest {
        val full = thread(5)
        val source = pagedSource(full, pageSize = 2)
        val vm = viewModel(commentDataSource = source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.loadMoreComments()

        vm.retryComments()

        assertEquals(full.take(2), (vm.uiState.value as PostDetailUiState.Loaded).comments)
        assertEquals(listOf("p1" to null, "p1" to "c2", "p1" to null), source.loadRequests)
    }

    // A reload that lands mid-flight has started the thread over, so the page still on the wire
    // describes a window that no longer follows what is on screen — appending it would show the
    // reader comments from two different threads, and duplicate keys in the list.
    @Test
    fun `a page a reload has moved past is not appended`() = runTest {
        val full = thread(5)
        val source = pagedSource(full, pageSize = 2)
        val vm = viewModel(commentDataSource = source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val newest = seedComment.copy(id = "c0", text = "Posted since")
        source.whileLoading = {
            // Only the load-more re-enters; the reload it triggers must not recurse.
            source.whileLoading = null
            source.comments = mapOf("p1" to (listOf(newest) + full))
            vm.retryComments()
            // Hands the thread over so the reload finishes here, while this page is still on the
            // wire — which is the whole of what this test is about.
            yield()
        }

        vm.loadMoreComments()

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertEquals(listOf(newest, full[0]), state.comments)
    }

    @Test
    fun `state updates emit to subscribers after addComment`() = runTest {
        val vm = viewModel(comments = emptyMap())
        val collected = mutableListOf<PostDetailUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect { collected.add(it) }
        }

        vm.addComment("First comment")

        val last = collected.last() as PostDetailUiState.Loaded
        assertEquals(1, last.comments.size)
        assertEquals("First comment", last.comments.first().text)
    }

    @Test
    fun `comment state is scoped to this post's id`() = runTest {
        val vm = viewModel(
            comments = mapOf("p1" to listOf(seedComment)),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.addComment("Only for p1")

        val state = vm.uiState.value as PostDetailUiState.Loaded
        assertEquals(2, state.comments.size)
    }
}

/** A [CommentDataSource] whose load suspends on [gate], so a test can observe the in-flight state. */
private class GatedCommentDataSource(
    private val gate: CompletableDeferred<Unit>,
    private val result: List<Comment>,
) : CommentDataSource {

    override suspend fun loadComments(postId: PostId, cursor: String?): CommentPage {
        gate.await()
        return CommentPage(comments = result, nextCursor = null, hasMore = false)
    }

    override suspend fun addComment(postId: PostId, text: String): Comment =
        throw UnsupportedOperationException()

    override suspend fun setLike(
        postId: PostId,
        commentId: CommentId,
        liked: Boolean,
    ): LikeState = throw UnsupportedOperationException()
}
