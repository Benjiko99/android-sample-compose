package uno.lux.sample.app

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uno.lux.sample.app.navigation.Navigator
import uno.lux.sample.app.navigation.Screen

class ShellViewModelTest {

    private val backStack = mutableListOf<NavKey>(Screen.Shell)
    private val navigator = Navigator().apply { attach(backStack) }
    private val viewModel = ShellViewModel(navigator)

    @Test
    fun `an action destination pushes its screen over the shell`() {
        viewModel.openDestination(Screen.CreatePost)

        assertEquals(listOf(Screen.Shell, Screen.CreatePost), backStack)
    }

    @Test
    fun `re-selecting an action destination does not stack a second copy`() {
        viewModel.openDestination(Screen.CreatePost)
        viewModel.openDestination(Screen.CreatePost)

        assertEquals(listOf(Screen.Shell, Screen.CreatePost), backStack)
    }

    @Test
    fun `CREATE is the only destination that navigates`() {
        val navigating = AppDestinations.entries.filter { it.screen != null }

        assertEquals(listOf(AppDestinations.CREATE), navigating)
        assertEquals(Screen.CreatePost, AppDestinations.CREATE.screen)
    }

    @Test
    fun `the tab destinations carry no screen, so selecting one never navigates`() {
        assertNull(AppDestinations.HOME.screen)
        assertNull(AppDestinations.PROFILE.screen)
    }
}
