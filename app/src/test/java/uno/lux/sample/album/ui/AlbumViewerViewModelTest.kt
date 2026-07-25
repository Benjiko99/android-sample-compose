package uno.lux.sample.album.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.app.navigation.Navigator
import uno.lux.sample.app.navigation.Screen
import uno.lux.sample.testing.backStackOf
import uno.lux.sample.testing.screens

class AlbumViewerViewModelTest {

    private val backStack = backStackOf(
        Screen.Shell,
        Screen.AlbumViewer(listOf("https://example.test/1.jpg"), initialIndex = 0),
    )
    private val navigator = Navigator().apply { attach(backStack) }

    @Test
    fun `goBack pops the viewer`() {
        AlbumViewerViewModel(navigator).goBack()

        assertEquals(listOf(Screen.Shell), backStack.screens())
    }
}
