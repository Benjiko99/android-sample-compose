package uno.lux.sample.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pins the guarantee the whole navigation design rests on: **the back stack survives process
 * death**. An app killed in the background comes back on the page it was killed on, because the
 * [Screen] keys are `@Serializable` and `rememberNavBackStack` saves them into the instance state.
 *
 * [StateRestorationTester.emulateSavedInstanceStateRestore] is what makes that testable without a
 * device kill: it saves the registry, throws the composition away, and rebuilds it from the saved
 * state — the same round trip the platform performs when it restarts a killed app. What it cannot
 * emulate is the *other* half of a real restart, that every in-memory store comes back empty; the
 * screens' own cold-start loading is covered by their ViewModel tests.
 *
 * The host below mirrors `MosaicApp`'s wiring — a `rememberNavBackStack` owned by the composition
 * and attached to a [Navigator] — rather than using it directly, which would drag in Hilt and the
 * network for a question that is purely about the keys.
 */
@RunWith(AndroidJUnit4::class)
class BackStackRestorationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val navigator = Navigator()
    private var backStack: List<NavKey> = emptyList()

    /** One of every [Screen], to catch a key that stops being serializable when a field is added. */
    private val everyScreen = listOf(
        Screen.Profile(userId = "u2"),
        Screen.Settings,
        Screen.EditProfile,
        Screen.CreatePost,
        Screen.PostDetail(postId = "p1"),
        Screen.FullscreenVideo(url = "https://example.test/v.mp4", title = "Clip"),
        Screen.AlbumViewer(images = listOf("https://example.test/1.jpg"), initialIndex = 0),
    )

    @Test
    fun restoresThePageThatWasOnTop() {
        val tester = startHost()

        push(Screen.Profile(userId = "u2"))
        assertTopIs(Screen.Profile(userId = "u2"))

        tester.emulateSavedInstanceStateRestore()

        assertTopIs(Screen.Profile(userId = "u2"))
    }

    @Test
    fun restoresTheEntriesUnderneathToo() {
        val tester = startHost()
        push(Screen.Profile(userId = "u2"))
        push(Screen.PostDetail(postId = "p1"))

        tester.emulateSavedInstanceStateRestore()

        // Backing out of the restored page walks the stack that was there before the restart,
        // rather than dropping straight to the root.
        assertTopIs(Screen.PostDetail(postId = "p1"))
        goBack()
        assertTopIs(Screen.Profile(userId = "u2"))
        goBack()
        assertTopIs(Screen.Shell)
    }

    @Test
    fun restoresEveryScreenKeyWithItsArguments() {
        val tester = startHost()
        everyScreen.forEach { push(it) }

        tester.emulateSavedInstanceStateRestore()

        composeTestRule.runOnIdle {
            assertEquals(listOf<NavKey>(Screen.Shell) + everyScreen, backStack)
        }
    }

    /** The saveable-state decorator's half of the deal: state *inside* an entry comes back too. */
    @Test
    fun restoresSaveableStateHeldInsideAnEntry() {
        val tester = startHost()
        push(Screen.Profile(userId = "u2"))

        composeTestRule.onNodeWithTag(CounterTag).performClick()
        composeTestRule.onNodeWithTag(CounterTag).assertTextEquals("1")

        tester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithTag(CounterTag).assertTextEquals("1")
    }

    // ── host ──────────────────────────────────────────────────────────────────

    private fun startHost(): StateRestorationTester {
        val tester = StateRestorationTester(composeTestRule)
        tester.setContent {
            TestNavHost(navigator = navigator, onBackStack = { backStack = it })
        }

        return tester
    }

    private fun push(screen: Screen) = composeTestRule.runOnIdle { navigator.goTo(screen) }

    private fun goBack() = composeTestRule.runOnIdle { navigator.goBack() }

    private fun assertTopIs(screen: Screen) {
        composeTestRule.onNodeWithTag(CurrentKeyTag).assertTextEquals(screen.toString())
    }
}

private const val CurrentKeyTag = "current_key"
private const val CounterTag = "counter"

/**
 * A stand-in for `MosaicApp`: the same composition-owned back stack, [Navigator] attachment and
 * entry decorators, with every key rendered as its own name instead of a real screen.
 */
@Composable
private fun TestNavHost(navigator: Navigator, onBackStack: (List<NavKey>) -> Unit) {
    val backStack = rememberNavBackStack(Screen.Shell)

    // Handing the stack out from the effect, not the composition body, so the test reads the
    // instance that is actually attached — a restore brings a new one, and re-runs this.
    DisposableEffect(navigator, backStack) {
        navigator.attach(backStack)
        onBackStack(backStack)
        onDispose { navigator.detach(backStack) }
    }

    NavDisplay(
        backStack = backStack,
        onBack = navigator::goBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider(
            fallback = { key -> NavEntry(key) { KeyLabel(key) } },
            builder = {},
        ),
    )
}

/** Names the entry on screen, and carries a saveable counter to prove per-entry state restores. */
@Composable
private fun KeyLabel(key: NavKey) {
    var taps by rememberSaveable { mutableIntStateOf(0) }

    Column {
        Text(text = key.toString(), modifier = Modifier.testTag(CurrentKeyTag))
        Text(
            text = taps.toString(),
            modifier = Modifier
                .testTag(CounterTag)
                .clickable { taps++ },
        )
    }
}
