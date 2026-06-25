package uno.lux.sample.ui.profile

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
import uno.lux.sample.data.post.Album
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostDataSource
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.profile.AlbumsPage
import uno.lux.sample.data.profile.PostsPage
import uno.lux.sample.data.profile.ProfileDataSource
import uno.lux.sample.data.profile.ProfileRefreshData
import uno.lux.sample.data.profile.ProfileRepository
import uno.lux.sample.data.profile.VideosPage
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserDataSource
import uno.lux.sample.data.user.UserRepository
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val ada = User(id = "u1", nickname = "Ada", handle = "@ada")
    private val grace = User(id = "u2", nickname = "Grace", handle = "@grace")
    private val post = Post(
        id = "p1",
        authorId = "u1",
        title = "Title",
        body = "Body",
        createdAt = Instant.EPOCH,
        likeCount = 10,
        commentCount = 2,
    )

    private fun viewModel(userId: String = "u1", currentUserId: String = "u1"): ProfileViewModel {
        val postRepo = PostRepository(FakePostDataSource())
        val userRepo = UserRepository(
            FakeUserDataSource(mapOf("u1" to ada, "u2" to grace)),
        )
        val profileRepo = ProfileRepository(
            FakeProfileDataSource(
                mapOf(
                    "u1" to ProfileRefreshData(
                        postsCount = 1, albumsCount = 1, videosCount = 1,
                        posts = listOf(post),
                        postCursor = null, postHasMore = false,
                        albums = listOf(Album(id = "a1", title = "Sketches", itemCount = 8)),
                        albumCursor = null, albumHasMore = false,
                        videos = listOf(Video(id = "v1", title = "Talk", durationSeconds = 95, viewCount = 40, videoUrl = "https://example.test/v1.mp4")),
                        videoCursor = null, videoHasMore = false,
                    ),
                    "u2" to ProfileRefreshData(
                        postsCount = 0, albumsCount = 0, videosCount = 0,
                        posts = emptyList(), postCursor = null, postHasMore = false,
                        albums = emptyList(), albumCursor = null, albumHasMore = false,
                        videos = emptyList(), videoCursor = null, videoHasMore = false,
                    ),
                ),
            ),
            postRepo,
        )
        return ProfileViewModel(
            profileRepository = profileRepo,
            postRepository = postRepo,
            userRepository = userRepo,
            currentUserId = currentUserId,
            userId = userId,
        )
    }

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
        assertEquals(ada, loaded.data.user)
        assertEquals(listOf(post), loaded.data.posts)
        assertEquals(1, loaded.data.profile.albumsCount)
        assertEquals(1, loaded.data.profile.videosCount)
    }

    @Test
    fun `uiState marks the signed-in user's own profile as current`() = runTest {
        val viewModel = viewModel(userId = "u1", currentUserId = "u1")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertTrue((viewModel.uiState.value as ProfileUiState.Loaded).isCurrentUser)
    }

    @Test
    fun `uiState marks another user's profile as not current`() = runTest {
        val viewModel = viewModel(userId = "u2", currentUserId = "u1")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertFalse((viewModel.uiState.value as ProfileUiState.Loaded).isCurrentUser)
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
    fun `onToggleLike likes the post through the shared entity store`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onToggleLike("p1")

        val liked = (viewModel.uiState.value as ProfileUiState.Loaded).data.posts.single()
        assertTrue(liked.isLiked)
        assertEquals(11, liked.likeCount)
    }

    @Test
    fun `onToggleBookmark bookmarks the post through the shared entity store`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onToggleBookmark("p1")

        val bookmarked = (viewModel.uiState.value as ProfileUiState.Loaded).data.posts.single()
        assertTrue(bookmarked.isBookmarked)
    }
}

private class FakePostDataSource : PostDataSource {
    override suspend fun toggleLike(post: Post) =
        post.copy(isLiked = !post.isLiked, likeCount = post.likeCount + if (!post.isLiked) 1 else -1)

    override suspend fun toggleBookmark(post: Post) =
        post.copy(isBookmarked = !post.isBookmarked)
}

private class FakeUserDataSource(private val users: Map<String, User> = emptyMap()) : UserDataSource {
    override suspend fun fetch(userId: String): User? = users[userId]
}

private class FakeProfileDataSource(
    private val data: Map<String, ProfileRefreshData> = emptyMap(),
) : ProfileDataSource {
    private val empty = ProfileRefreshData(
        postsCount = 0, albumsCount = 0, videosCount = 0,
        posts = emptyList(), postCursor = null, postHasMore = false,
        albums = emptyList(), albumCursor = null, albumHasMore = false,
        videos = emptyList(), videoCursor = null, videoHasMore = false,
    )

    override suspend fun refresh(userId: String) = data[userId] ?: empty
    override suspend fun loadMorePosts(userId: String, cursor: String?) = PostsPage(emptyList(), null, false)
    override suspend fun loadMoreAlbums(userId: String, cursor: String?) = AlbumsPage(emptyList(), null, false)
    override suspend fun loadMoreVideos(userId: String, cursor: String?) = VideosPage(emptyList(), null, false)
}
