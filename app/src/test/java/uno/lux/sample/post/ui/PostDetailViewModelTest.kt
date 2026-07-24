package uno.lux.sample.post.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.testing.ViewModelTest
import uno.lux.sample.comment.Comment
import uno.lux.sample.comment.data.CommentDataSource
import uno.lux.sample.comment.data.FakeCommentDataSource
import uno.lux.sample.post.data.FakePostDataSource
import uno.lux.sample.comment.data.CommentRepository
import uno.lux.sample.post.Post
import uno.lux.sample.post.PostId
import uno.lux.sample.post.data.PostRepository
import uno.lux.sample.post.PostWithAuthor
import uno.lux.sample.user.data.FakeUserDataSource
import uno.lux.sample.user.User
import uno.lux.sample.user.data.UserRepository
import uno.lux.sample.app.navigation.Navigator
import uno.lux.sample.app.navigation.Screen
import uno.lux.sample.app.util.AppError
import uno.lux.sample.testing.testPostUrl
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
    private val backStack = mutableListOf<NavKey>(Screen.Shell, Screen.PostDetail("p1"))
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
        assertEquals(listOf<NavKey>(Screen.Shell), backStack)
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

    // ── navigation ────────────────────────────────────────────────────────────

    @Test
    fun `goBack pops the detail page`() {
        viewModel().goBack()

        assertEquals(listOf<NavKey>(Screen.Shell), backStack)
    }

    @Test
    fun `openProfile pushes the author's profile page`() {
        viewModel().openProfile("u2")

        assertEquals(Screen.Profile("u2"), backStack.last())
    }

    @Test
    fun `openAlbum pushes the album viewer at the tapped image`() {
        val images = listOf("https://example.test/1.jpg")

        viewModel().openAlbum(images, initialIndex = 0)

        assertEquals(Screen.AlbumViewer(images, initialIndex = 0), backStack.last())
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
        val vm = viewModel()
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

    @Test
    fun `onToggleCommentLike likes a comment in the thread`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onToggleCommentLike("c1")

        val state = vm.uiState.value as PostDetailUiState.Loaded
        val liked = state.comments.single()
        assertTrue(liked.isLiked)
        assertEquals(3, liked.likeCount)
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

    override suspend fun loadComments(postId: PostId): List<Comment> {
        gate.await()
        return result
    }

    override suspend fun addComment(postId: PostId, text: String): Comment =
        throw UnsupportedOperationException()

    override suspend fun toggleLike(postId: PostId, comment: Comment): Comment =
        throw UnsupportedOperationException()
}

