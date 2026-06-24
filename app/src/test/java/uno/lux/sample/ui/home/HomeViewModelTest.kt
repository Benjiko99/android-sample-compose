package uno.lux.sample.ui.home

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import uno.lux.sample.MainDispatcherRule
import uno.lux.sample.data.post.FeedRepository
import uno.lux.sample.data.post.InMemoryFeedRepository
import uno.lux.sample.data.post.InMemoryPostRepository
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.user.InMemoryUserRepository
import uno.lux.sample.data.user.User
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val author = User(id = "u1", nickname = "Ada", handle = "@ada")
    private val post = Post(
        id = "p1",
        authorId = "u1",
        title = "Title",
        body = "Body",
        createdAt = Instant.EPOCH,
        likeCount = 10,
        commentCount = 2,
    )

    private fun viewModel() = HomeViewModel(
        feedRepository = InMemoryFeedRepository(listOf(post.id)),
        postRepository = InMemoryPostRepository(listOf(post)),
        userRepository = InMemoryUserRepository(listOf(author)),
    )

    @Test
    fun `uiState is Loading until something collects it`() {
        assertEquals(HomeUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `uiState exposes the feed once collected`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val feed = viewModel.uiState.value as HomeUiState.Feed
        assertEquals(listOf(PostCardData(post, author)), feed.posts)
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
        assertTrue(feed.posts.single().post.isLiked)
        assertEquals(11, feed.posts.single().post.likeCount)
    }

    @Test
    fun `onToggleBookmark bookmarks the post through the repository`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onToggleBookmark("p1")

        val feed = viewModel.uiState.value as HomeUiState.Feed
        assertTrue(feed.posts.single().post.isBookmarked)
    }

    @Test
    fun `refresh raises isRefreshing until the feed repository finishes`() = runTest {
        val feedRepository = FakeFeedRepository(suspendOnRefresh = true)
        val viewModel = HomeViewModel(feedRepository, InMemoryPostRepository(), InMemoryUserRepository())

        assertFalse(viewModel.isRefreshing.value)
        viewModel.refresh()
        assertTrue(viewModel.isRefreshing.value)
        feedRepository.completeRefresh()
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `endReached is false when the feed repository reports hasMore`() = runTest {
        val feedRepository = FakeFeedRepository(initialHasMore = true)
        val viewModel = HomeViewModel(feedRepository, InMemoryPostRepository(), InMemoryUserRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val feed = viewModel.uiState.value as HomeUiState.Feed
        assertFalse(feed.endReached)
    }
}

// TODO: Fake repositories would perhaps be better replaced with a FakeDataSource, like we discussed
//  in another TODO comment, regarding using DataSources, so we don't duplicate the implementation
//  of the whole repository.
// TODO: We should perhaps get rid of InMemory implementations of repositories, what is their purpose?
//  They're kinda nice in that this is a portfolio project, and it lets us not need a server,
//  but it's better to show off how we write network code, and it's also closer to what production
//  code would look like, so we should just drop them, and host a server. InMemory repositories
//  aren't even needed to back up tests, they use Fake repositories.
private class FakeFeedRepository(
    initialHasMore: Boolean = false,
    private val suspendOnRefresh: Boolean = false,
) : FeedRepository {
    override val postIds: StateFlow<List<String>> = MutableStateFlow(emptyList())
    override val hasMore: StateFlow<Boolean> = MutableStateFlow(initialHasMore)
    private val refreshGate = CompletableDeferred<Unit>()

    override suspend fun refresh() { if (suspendOnRefresh) refreshGate.await() }
    override suspend fun loadMore() {}

    fun completeRefresh() = refreshGate.complete(Unit)
}
