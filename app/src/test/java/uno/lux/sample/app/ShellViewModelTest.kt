package uno.lux.sample.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uno.lux.sample.app.navigation.Navigator
import uno.lux.sample.app.navigation.Screen
import uno.lux.sample.app.ui.AppDestinations
import uno.lux.sample.app.ui.ShellViewModel
import uno.lux.sample.testing.backStackOf
import uno.lux.sample.testing.screens

class ShellViewModelTest {

    private val backStack = backStackOf(Screen.Shell)
    private val navigator = Navigator().apply { attach(backStack) }
    private val viewModel = ShellViewModel(navigator)

    @Test
    fun `an action destination pushes its screen over the shell`() {
        viewModel.openDestination(Screen.CreatePost)

        assertEquals(listOf(Screen.Shell, Screen.CreatePost), backStack.screens())
    }

    @Test
    fun `re-selecting an action destination does not stack a second copy`() {
        viewModel.openDestination(Screen.CreatePost)
        viewModel.openDestination(Screen.CreatePost)

        assertEquals(listOf(Screen.Shell, Screen.CreatePost), backStack.screens())
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
