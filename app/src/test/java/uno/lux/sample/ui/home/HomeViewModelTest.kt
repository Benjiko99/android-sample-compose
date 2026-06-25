package uno.lux.sample.ui.home

import kotlinx.coroutines.CompletableDeferred
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
import uno.lux.sample.data.post.FeedDataSource
import uno.lux.sample.data.post.FeedPage
import uno.lux.sample.data.post.FeedRepository
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostDataSource
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserDataSource
import uno.lux.sample.data.user.UserRepository
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

    private fun viewModel(
        feedDataSource: FeedDataSource = FakeFeedDataSource(FeedPage(listOf(post), listOf(author), null, false)),
    ): HomeViewModel {
        val postRepo = PostRepository(FakePostDataSource())
        val userRepo = UserRepository(FakeUserDataSource())
        val feedRepo = FeedRepository(feedDataSource, postRepo, userRepo)
        return HomeViewModel(feedRepo, postRepo, userRepo)
    }

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
    fun `refresh raises isRefreshing until the feed data source finishes`() = runTest {
        val dataSource = SuspendingFeedDataSource()
        val viewModel = viewModel(feedDataSource = dataSource)

        assertFalse(viewModel.isRefreshing.value)
        viewModel.refresh()
        assertTrue(viewModel.isRefreshing.value)
        dataSource.complete()
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `endReached is false when the feed reports hasMore`() = runTest {
        val viewModel = viewModel(
            feedDataSource = FakeFeedDataSource(FeedPage(emptyList(), emptyList(), "cursor", true)),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val feed = viewModel.uiState.value as HomeUiState.Feed
        assertFalse(feed.endReached)
    }
}

private class FakeFeedDataSource(private val page: FeedPage) : FeedDataSource {
    override suspend fun fetch(cursor: String?) = page
}

private class SuspendingFeedDataSource : FeedDataSource {
    private val gate = CompletableDeferred<Unit>()
    override suspend fun fetch(cursor: String?): FeedPage {
        gate.await()
        return FeedPage(emptyList(), emptyList(), null, false)
    }
    fun complete() = gate.complete(Unit)
}

private class FakePostDataSource : PostDataSource {
    override suspend fun toggleLike(post: Post) =
        post.copy(isLiked = !post.isLiked, likeCount = post.likeCount + if (!post.isLiked) 1 else -1)

    override suspend fun toggleBookmark(post: Post) =
        post.copy(isBookmarked = !post.isBookmarked)
}

private class FakeUserDataSource : UserDataSource {
    override suspend fun fetch(userId: String): User? = null
}
