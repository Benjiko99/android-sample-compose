package uno.lux.sample.ui.create

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.ViewModelTest
import uno.lux.sample.data.post.FakeFeedDataSource
import uno.lux.sample.data.post.FakePostDataSource
import uno.lux.sample.data.post.FeedRepository
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.user.FakeUserDataSource
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.navigation.Screen
import uno.lux.sample.util.AppError
import java.io.IOException

class CreatePostViewModelTest : ViewModelTest() {

    private val backStack = mutableListOf<NavKey>(Screen.Shell)
    private val navigator = Navigator().apply { attach(backStack) }

    private data class Fixture(
        val viewModel: CreatePostViewModel,
        val postDataSource: FakePostDataSource,
    )

    private fun fixture(createError: Exception? = null): Fixture {
        val postDataSource = FakePostDataSource().apply { this.createError = createError }
        val feedRepository = FeedRepository(
            dataSource = FakeFeedDataSource(),
            postRepository = PostRepository(postDataSource),
            userRepository = UserRepository(FakeUserDataSource()),
        )

        return Fixture(CreatePostViewModel(feedRepository, navigator), postDataSource)
    }

    private fun CreatePostViewModel.fillIn(title: String = "Title", body: String = "Body") {
        onTitleChange(title)
        onBodyChange(body)
    }

    @Test
    fun `the form starts blank and cannot be published`() = runTest {
        val state = fixture().viewModel.uiState.first()

        assertTrue(state.form.isEmpty)
        assertFalse(state.form.canPublish)
        assertFalse(state.isPublishing)
    }

    @Test
    fun `editing the fields updates the form`() = runTest {
        val viewModel = fixture().viewModel

        viewModel.fillIn(title = "Engine sketches", body = "Carry mechanism works.")

        val form = viewModel.uiState.first().form
        assertEquals("Engine sketches", form.title)
        assertEquals("Carry mechanism works.", form.body)
        assertTrue(form.canPublish)
    }

    @Test
    fun `a blank field keeps publishing disabled`() = runTest {
        val viewModel = fixture().viewModel

        viewModel.fillIn(title = "   ", body = "Body")

        assertFalse(viewModel.uiState.first().form.canPublish)
    }

    @Test
    fun `the fields are capped at the backend's limits`() = runTest {
        val viewModel = fixture().viewModel

        viewModel.fillIn(title = "t".repeat(CreatePostTitleMaxLength + 10), body = "b")

        assertEquals(CreatePostTitleMaxLength, viewModel.uiState.first().form.title.length)
    }

    @Test
    fun `publish sends the trimmed draft`() = runTest {
        val (viewModel, dataSource) = fixture()
        viewModel.fillIn(title = "  Title  ", body = "  Body  ")

        viewModel.publish()

        assertEquals("Title", dataSource.lastDraft?.title)
        assertEquals("Body", dataSource.lastDraft?.body)
    }

    @Test
    fun `a published post clears the form and opens its detail page`() = runTest {
        val viewModel = fixture().viewModel
        viewModel.fillIn()

        viewModel.publish()

        assertTrue(viewModel.uiState.first().form.isEmpty)
        assertFalse(viewModel.uiState.first().isPublishing)
        assertEquals(listOf(Screen.Shell, Screen.PostDetail("p-new")), backStack)
    }

    @Test
    fun `publish is a no-op while the form is incomplete`() = runTest {
        val (viewModel, dataSource) = fixture()
        viewModel.onTitleChange("Title")

        viewModel.publish()

        assertNull(dataSource.lastDraft)
        assertEquals(listOf(Screen.Shell), backStack)
    }

    @Test
    fun `a failed publish keeps the typed text and surfaces the error`() = runTest {
        val viewModel = fixture(createError = IOException("offline")).viewModel
        viewModel.fillIn(title = "Title", body = "Body")

        viewModel.publish()

        val state = viewModel.uiState.first()
        assertEquals("Title", state.form.title)
        assertEquals(AppError.Unknown, state.publishError)
        assertFalse(state.isPublishing)
        assertEquals(listOf(Screen.Shell), backStack)
    }

    @Test
    fun `discard clears the form`() = runTest {
        val viewModel = fixture().viewModel
        viewModel.fillIn()

        viewModel.discard()

        assertTrue(viewModel.uiState.first().form.isEmpty)
    }

    @Test
    fun `openSettings does not stack a second settings page`() = runTest {
        val viewModel = fixture().viewModel

        viewModel.openSettings()
        viewModel.openSettings()

        assertEquals(listOf(Screen.Shell, Screen.Settings), backStack)
    }
}
