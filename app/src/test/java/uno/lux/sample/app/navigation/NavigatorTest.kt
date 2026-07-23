package uno.lux.sample.app.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigatorTest {

    private val navigator = Navigator()
    private val backStack = mutableListOf<NavKey>(Screen.Shell)

    @Test
    fun `goTo pushes onto the attached back stack`() {
        navigator.attach(backStack)

        navigator.goTo(Screen.Profile("u1"))
        navigator.goTo(Screen.Settings)

        assertEquals(listOf(Screen.Shell, Screen.Profile("u1"), Screen.Settings), backStack)
    }

    @Test
    fun `goTo allows pushing a screen equal to the current top`() {
        navigator.attach(backStack)

        navigator.goTo(Screen.Profile("u1"))
        navigator.goTo(Screen.Profile("u1"))

        assertEquals(listOf(Screen.Shell, Screen.Profile("u1"), Screen.Profile("u1")), backStack)
    }

    @Test
    fun `goToSingleTop pushes when the screen is not on top`() {
        navigator.attach(backStack)

        navigator.goToSingleTop(Screen.Settings)

        assertEquals(listOf(Screen.Shell, Screen.Settings), backStack)
    }

    @Test
    fun `goToSingleTop is a no-op when the screen already sits on top`() {
        backStack.add(Screen.Settings)
        navigator.attach(backStack)

        navigator.goToSingleTop(Screen.Settings)

        assertEquals(listOf(Screen.Shell, Screen.Settings), backStack)
    }

    @Test
    fun `goToSingleTop pushes when the same screen is buried but not on top`() {
        backStack.add(Screen.Settings)
        backStack.add(Screen.Profile("u1"))
        navigator.attach(backStack)

        navigator.goToSingleTop(Screen.Settings)

        assertEquals(
            listOf(Screen.Shell, Screen.Settings, Screen.Profile("u1"), Screen.Settings),
            backStack,
        )
    }

    @Test
    fun `goBack pops the top entry`() {
        backStack.add(Screen.Settings)
        navigator.attach(backStack)

        navigator.goBack()

        assertEquals(listOf<NavKey>(Screen.Shell), backStack)
    }

    @Test
    fun `replaceTop swaps the top entry, leaving what sat below it`() {
        backStack.add(Screen.CreatePost)
        navigator.attach(backStack)

        navigator.replaceTop(Screen.PostDetail("p1"))

        assertEquals(listOf(Screen.Shell, Screen.PostDetail("p1")), backStack)
    }

    @Test
    fun `replaceTop leaves entries below the top untouched`() {
        backStack.add(Screen.Profile("u1"))
        backStack.add(Screen.CreatePost)
        navigator.attach(backStack)

        navigator.replaceTop(Screen.PostDetail("p1"))

        assertEquals(
            listOf(Screen.Shell, Screen.Profile("u1"), Screen.PostDetail("p1")),
            backStack,
        )
    }

    @Test
    fun `navigation is dropped while no back stack is attached`() {
        navigator.goTo(Screen.Settings)
        navigator.replaceTop(Screen.Settings)
        navigator.goBack()

        assertEquals(listOf<NavKey>(Screen.Shell), backStack)
    }

    @Test
    fun `navigation is dropped after the attached stack detaches`() {
        navigator.attach(backStack)
        navigator.detach(backStack)

        navigator.goTo(Screen.Settings)

        assertEquals(listOf<NavKey>(Screen.Shell), backStack)
    }

    @Test
    fun `detaching a stale stack keeps the current one attached`() {
        val staleStack = mutableListOf<NavKey>(Screen.Shell)
        navigator.attach(staleStack)
        navigator.attach(backStack)

        navigator.detach(staleStack)
        navigator.goTo(Screen.Settings)

        assertEquals(listOf(Screen.Shell, Screen.Settings), backStack)
    }
}
