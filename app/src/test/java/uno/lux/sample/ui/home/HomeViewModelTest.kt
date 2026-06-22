package uno.lux.sample.ui.home

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import uno.lux.sample.MainDispatcherRule
import uno.lux.sample.data.User
import uno.lux.sample.data.post.InMemoryPostRepository
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostRepository
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val post = Post(
        id = "p1",
        author = User(id = "u1", nickname = "Ada", handle = "@ada"),
        title = "Title",
        body = "Body",
        createdAt = Instant.EPOCH,
        likeCount = 10,
        commentCount = 2,
    )

    private fun viewModel() = HomeViewModel(InMemoryPostRepository(listOf(post)))

    @Test
    fun `uiState is Loading until something collects it`() {
        assertEquals(HomeUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `uiState exposes the repository feed once collected`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val feed = viewModel.uiState.value as HomeUiState.Feed
        assertEquals(listOf(post), feed.posts)
        assertTrue(feed.endReached)
    }

    @Test
    fun `onToggleLike likes the post through the repository`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onToggleLike("p1")

        val feed = viewModel.uiState.value as HomeUiState.Feed
        assertTrue(feed.posts.single().isLiked)
        assertEquals(11, feed.posts.single().likeCount)
    }

    @Test
    fun `onToggleBookmark bookmarks the post through the repository`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onToggleBookmark("p1")

        val feed = viewModel.uiState.value as HomeUiState.Feed
        assertTrue(feed.posts.single().isBookmarked)
    }

    @Test
    fun `refresh raises isRefreshing until the repository finishes`() = runTest {
        val repository = FakePostRepository()
        val viewModel = HomeViewModel(repository)

        assertFalse(viewModel.isRefreshing.value)
        viewModel.refresh()
        assertTrue(viewModel.isRefreshing.value)
        repository.completeRefresh()
        assertFalse(viewModel.isRefreshing.value)
    }
}

/** A [PostRepository] whose refresh suspends until [completeRefresh], to observe refresh state. */
private class FakePostRepository : PostRepository {
    override val posts: Flow<List<Post>> = MutableStateFlow<List<Post>>(emptyList())
    private val refreshGate = CompletableDeferred<Unit>()

    override suspend fun refresh() = refreshGate.await()

    fun completeRefresh() {
        refreshGate.complete(Unit)
    }

    override suspend fun toggleLike(postId: String) = Unit
    override suspend fun toggleBookmark(postId: String) = Unit
}
