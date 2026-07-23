package uno.lux.sample.ui.video

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.navigation.Screen

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
