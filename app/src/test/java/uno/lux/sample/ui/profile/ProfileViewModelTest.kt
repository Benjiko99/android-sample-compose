package uno.lux.sample.ui.profile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import uno.lux.sample.MainDispatcherRule
import uno.lux.sample.data.Album
import uno.lux.sample.data.InMemoryPostRepository
import uno.lux.sample.data.InMemoryProfileRepository
import uno.lux.sample.data.Post
import uno.lux.sample.data.User
import uno.lux.sample.data.Video
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val ada = User(id = "u1", nickname = "Ada", handle = "@ada")
    private val post = Post(
        id = "p1",
        author = ada,
        title = "Title",
        body = "Body",
        createdAt = Instant.EPOCH,
        likeCount = 10,
        commentCount = 2,
    )

    private fun viewModel(userId: String = "u1") = ProfileViewModel(
        repository = InMemoryProfileRepository(
            users = listOf(ada),
            postRepository = InMemoryPostRepository(listOf(post)),
            albumsByUser = mapOf("u1" to listOf(Album(id = "a1", title = "Sketches", itemCount = 8))),
            videosByUser = mapOf("u1" to listOf(Video(id = "v1", title = "Talk", durationSeconds = 95, viewCount = 40, videoUrl = "https://example.test/v1.mp4"))),
        ),
        userId = userId,
    )

    @Test
    fun `uiState is Loading until something collects it`() {
        assertEquals(ProfileUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `uiState exposes the loaded profile once collected`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val loaded = viewModel.uiState.value as ProfileUiState.Loaded
        assertEquals(ada, loaded.profile.user)
        assertEquals(listOf(post), loaded.profile.posts)
        assertEquals(1, loaded.profile.albumsCount)
        assertEquals(1, loaded.profile.videosCount)
    }

    @Test
    fun `uiState is NotFound for an unknown user`() = runTest {
        val viewModel = viewModel(userId = "nobody")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(ProfileUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun `onToggleLike likes the post through the repository`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onToggleLike("p1")

        val liked = (viewModel.uiState.value as ProfileUiState.Loaded).profile.posts.single()
        assertTrue(liked.isLiked)
        assertEquals(11, liked.likeCount)
    }

    @Test
    fun `onToggleBookmark bookmarks the post through the repository`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onToggleBookmark("p1")

        val bookmarked = (viewModel.uiState.value as ProfileUiState.Loaded).profile.posts.single()
        assertTrue(bookmarked.isBookmarked)
    }
}
