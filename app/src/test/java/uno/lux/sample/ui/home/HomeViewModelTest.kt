package uno.lux.sample.ui.home

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.ViewModelTest
import uno.lux.sample.ui.post.PostCardData
import uno.lux.sample.data.post.FakeFeedDataSource
import uno.lux.sample.data.post.FeedDataSource
import uno.lux.sample.data.post.FeedPage
import uno.lux.sample.data.post.FeedRepository
import uno.lux.sample.data.post.FakePostDataSource
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.user.FakeUserDataSource
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.navigation.Screen
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : ViewModelTest() {

    private val author = User(id = "u1", nickname = "Ada", handle = "@ada")
    private val author2 = User(id = "u2", nickname = "Bob", handle = "@bob")
    private val post = Post(
        id = "p1",
        authorId = "u1",
        title = "Title",
        body = "Body",
        createdAt = Instant.EPOCH,
        likeCount = 10,
        commentCount = 2,
    )
    private val post2 = Post(
        id = "p2",
        authorId = "u2",
        title = "Title 2",
        body = "Body 2",
        createdAt = Instant.EPOCH,
        likeCount = 5,
        commentCount = 0,
    )

    // The real Navigator drives a plain list, so navigation tests assert on the stack directly.
    private val backStack = mutableListOf<NavKey>(Screen.Shell)
    private val navigator = Navigator().apply { attach(backStack) }

    private fun viewModel(
        feedDataSource: FeedDataSource = FakeFeedDataSource(listOf(FeedPage(listOf(post), listOf(author), null, false))),
    ): HomeViewModel {
        val postRepo = PostRepository(FakePostDataSource())
        val userRepo = UserRepository(FakeUserDataSource())
        val feedRepo = FeedRepository(feedDataSource, postRepo, userRepo)
        return HomeViewModel(feedRepo, postRepo, userRepo, navigator)
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
    fun `uiState stays Loading while the initial load is active`() = runTest {
        val dataSource = SuspendingFeedDataSource()
        val viewModel = viewModel(feedDataSource = dataSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(HomeUiState.Loading, viewModel.uiState.value)

        dataSource.complete()

        assertTrue(viewModel.uiState.value is HomeUiState.Feed)
    }

    @Test
    fun `uiState shows Error when the initial load fails`() = runTest {
        val viewModel = viewModel(feedDataSource = FailingFeedDataSource())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertTrue(viewModel.uiState.value is HomeUiState.Error)
    }

    @Test
    fun `retry shows Loading while reloading after the feed was already loaded`() = runTest {
        val dataSource = SucceedOnceThenSuspendFeedDataSource(
            firstPage = FeedPage(listOf(post), listOf(author), null, false),
        )
        val viewModel = viewModel(feedDataSource = dataSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        assertTrue(viewModel.uiState.value is HomeUiState.Feed)

        viewModel.retry()

        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
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
    fun `PostCardData instance is preserved for unchanged posts on a like toggle`() = runTest {
        val viewModel = viewModel(
            feedDataSource = FakeFeedDataSource(
                listOf(FeedPage(listOf(post, post2), listOf(author, author2), null, false)),
            ),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        val cardBefore = (viewModel.uiState.value as HomeUiState.Feed).posts[1]

        viewModel.onToggleLike(post.id)

        val cardAfter = (viewModel.uiState.value as HomeUiState.Feed).posts[1]
        assertSame(cardBefore, cardAfter)
    }

    @Test
    fun `openSettings pushes the settings page`() {
        viewModel().openSettings()

        assertEquals(Screen.Settings, backStack.last())
    }

    @Test
    fun `openSettings does not stack a second settings page`() {
        val viewModel = viewModel()

        viewModel.openSettings()
        viewModel.openSettings()

        assertEquals(listOf(Screen.Shell, Screen.Settings), backStack)
    }

    @Test
    fun `openProfile pushes the author's profile page`() {
        viewModel().openProfile("u1")

        assertEquals(Screen.Profile("u1"), backStack.last())
    }

    @Test
    fun `openPost pushes the post's detail page`() {
        viewModel().openPost("p1")

        assertEquals(Screen.PostDetail("p1"), backStack.last())
    }

    @Test
    fun `openVideo pushes the fullscreen player for that video`() {
        val video = Video(
            id = "v1",
            title = "Talk",
            durationSeconds = 95,
            viewCount = 40,
            videoUrl = "https://example.test/v1.mp4",
        )

        viewModel().openVideo(video)

        assertEquals(
            Screen.FullscreenVideo("v1", "https://example.test/v1.mp4", "Talk"),
            backStack.last(),
        )
    }

    @Test
    fun `openAlbum pushes the album viewer at the tapped image`() {
        val images = listOf("https://example.test/1.jpg", "https://example.test/2.jpg")

        viewModel().openAlbum(images, initialIndex = 1)

        assertEquals(Screen.AlbumViewer(images, initialIndex = 1), backStack.last())
    }

    @Test
    fun `endReached is false when the feed reports hasMore`() = runTest {
        val viewModel = viewModel(
            feedDataSource = FakeFeedDataSource(listOf(FeedPage(emptyList(), emptyList(), "cursor", true))),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val feed = viewModel.uiState.value as HomeUiState.Feed
        assertFalse(feed.endReached)
    }
}

private class SucceedOnceThenSuspendFeedDataSource(
    private val firstPage: FeedPage,
) : FeedDataSource {
    private val gate = CompletableDeferred<Unit>()
    private var firstFetch = true

    override suspend fun fetch(cursor: String?): FeedPage {
        if (firstFetch) {
            firstFetch = false
            return firstPage
        }
        gate.await()
        return FeedPage(emptyList(), emptyList(), null, false)
    }
}

private class FailingFeedDataSource : FeedDataSource {
    override suspend fun fetch(cursor: String?): FeedPage = throw RuntimeException("network error")
}

private class SuspendingFeedDataSource : FeedDataSource {
    private val gate = CompletableDeferred<Unit>()
    override suspend fun fetch(cursor: String?): FeedPage {
        gate.await()
        return FeedPage(emptyList(), emptyList(), null, false)
    }
    fun complete() = gate.complete(Unit)
}

