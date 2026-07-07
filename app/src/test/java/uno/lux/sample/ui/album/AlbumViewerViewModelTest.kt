package uno.lux.sample.ui.album

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.navigation.Screen

class AlbumViewerViewModelTest {

    private val backStack = mutableListOf<NavKey>(
        Screen.Shell,
        Screen.AlbumViewer(listOf("https://example.test/1.jpg"), initialIndex = 0),
    )
    private val navigator = Navigator().apply { attach(backStack) }

    @Test
    fun `goBack pops the viewer`() {
        AlbumViewerViewModel(navigator).goBack()

        assertEquals(listOf<NavKey>(Screen.Shell), backStack)
    }
}
