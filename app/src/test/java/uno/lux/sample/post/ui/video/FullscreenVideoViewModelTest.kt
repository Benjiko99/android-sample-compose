package uno.lux.sample.post.ui.video

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.app.navigation.Navigator
import uno.lux.sample.app.navigation.Screen
import uno.lux.sample.video.ui.FullscreenVideoViewModel

class FullscreenVideoViewModelTest {

    private val backStack = mutableListOf<NavKey>(
        Screen.Shell,
        Screen.FullscreenVideo(url = "https://example.test/v1.mp4", title = "Talk"),
    )
    private val navigator = Navigator().apply { attach(backStack) }

    @Test
    fun `goBack pops the video page`() {
        FullscreenVideoViewModel(navigator).goBack()

        assertEquals(listOf<NavKey>(Screen.Shell), backStack)
    }
}
