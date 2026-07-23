package uno.lux.sample.ui.post

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
import uno.lux.sample.ViewModelTest
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.CommentDataSource
import uno.lux.sample.data.post.FakeCommentDataSource
import uno.lux.sample.data.post.FakePostDataSource
import uno.lux.sample.data.post.CommentRepository
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.user.FakeUserDataSource
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.navigation.Screen
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailViewModelTest : ViewModelTest() {

    private val currentUser = User(id = "me", nickname = "Me", handle = "@me")
    private val otherUser = User(id = "u2", nickname = "Other", handle = "@other")

    private val post = Post(
        id = "p1",
        url = "https://mosaic.test/p/p1",
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

    private fun viewModel(
        postId: String = "p1",
        posts: List<Post> = listOf(post),
        comments: Map<String, List<Comment>> = mapOf("p1" to listOf(seedComment)),
        commentDataSource: CommentDataSource = FakeCommentDataSource(currentUser, comments),
    ): PostDetailViewModel {
        val postRepo = PostRepository(FakePostDataSource())
        postRepo.ingest(posts)
        val userRepo = UserRepository(FakeUserDataSource())
        userRepo.ingest(listOf(currentUser, otherUser))
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

    @Test
    fun `uiState is NotFound when the postId is absent from the repository`() = runTest {
        val vm = viewModel(postId = "missing")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(PostDetailUiState.NotFound, vm.uiState.value)
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

