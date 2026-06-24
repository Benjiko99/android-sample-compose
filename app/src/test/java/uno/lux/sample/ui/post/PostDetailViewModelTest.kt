package uno.lux.sample.ui.post

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import uno.lux.sample.MainDispatcherRule
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.InMemoryCommentRepository
import uno.lux.sample.data.post.InMemoryPostRepository
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.user.InMemoryUserRepository
import uno.lux.sample.data.user.User
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val currentUser = User(id = "me", nickname = "Me", handle = "@me")
    private val otherUser = User(id = "u2", nickname = "Other", handle = "@other")

    private val post = Post(
        id = "p1",
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

    private fun viewModel(
        postId: String = "p1",
        posts: List<Post> = listOf(post),
        comments: Map<String, List<Comment>> = mapOf("p1" to listOf(seedComment)),
    ) = PostDetailViewModel(
        postRepository = InMemoryPostRepository(posts),
        commentRepository = InMemoryCommentRepository(currentUser, comments),
        userRepository = InMemoryUserRepository(listOf(currentUser, otherUser)),
        currentUser = currentUser,
        postId = postId,
    )

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
    }

    @Test
    fun `uiState is NotFound when the postId is absent from the repository`() = runTest {
        val vm = viewModel(postId = "missing")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(PostDetailUiState.NotFound, vm.uiState.value)
    }

    // ── composerUserName ──────────────────────────────────────────────────────

    @Test
    fun `composerUserName is the current user's nickname`() {
        assertEquals("Me", viewModel().composerUserName)
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
