package uno.lux.sample.ui

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.navigation.Screen

class PlaceholderViewModelTest {

    private val backStack = mutableListOf<NavKey>(Screen.Shell)
    private val navigator = Navigator().apply { attach(backStack) }

    @Test
    fun `openSettings pushes the settings page`() {
        PlaceholderViewModel(navigator).openSettings()

        assertEquals(listOf(Screen.Shell, Screen.Settings), backStack)
    }

    @Test
    fun `openSettings does not stack a second settings page`() {
        val viewModel = PlaceholderViewModel(navigator)

        viewModel.openSettings()
        viewModel.openSettings()

        assertEquals(listOf(Screen.Shell, Screen.Settings), backStack)
    }
}
