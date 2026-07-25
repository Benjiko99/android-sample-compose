package uno.lux.sample.video.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.app.navigation.Navigator
import uno.lux.sample.app.navigation.Screen
import uno.lux.sample.testing.backStackOf
import uno.lux.sample.testing.screens

class FullscreenVideoViewModelTest {

    private val backStack = backStackOf(
        Screen.Shell,
        Screen.FullscreenVideo(url = "https://example.test/v1.mp4", title = "Talk"),
    )
    private val navigator = Navigator().apply { attach(backStack) }

    @Test
    fun `goBack pops the video page`() {
        FullscreenVideoViewModel(navigator).goBack()

        assertEquals(listOf(Screen.Shell), backStack.screens())
    }
}
