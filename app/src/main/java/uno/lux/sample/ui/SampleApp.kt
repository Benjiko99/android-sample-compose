package uno.lux.sample.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import uno.lux.sample.R
import uno.lux.sample.ui.components.DividedNavigationSuiteScaffold
import uno.lux.sample.ui.home.HomeScreen
import uno.lux.sample.ui.navigation.Screen
import uno.lux.sample.ui.profile.ProfileScreen
import uno.lux.sample.ui.settings.SettingsScreen
import uno.lux.sample.ui.theme.LocalMosaicColors

/**
 * App shell. A Navigation 3 [NavDisplay] renders the top of a [rememberNavBackStack] back
 * stack of [Screen] keys: the tabbed [HomeNavShell] is the permanent root, and a user's
 * [ProfileScreen] or the [SettingsScreen] push over it as full-screen pages (settings stacks
 * over a profile, too). Pushes slide in from the right and pops slide back off — the
 * predictive back gesture included — while tab switches inside the shell stay a fade-through
 * between peers. The entry decorators give every page its own saveable-state and ViewModel
 * store, so a pushed page's ViewModel is created on push and cleared on pop.
 */
@Composable
fun SampleApp() {
    val backStack = rememberNavBackStack(Screen.Shell)
    // Guard against re-adding the page already on top (e.g. a double-tap on the gear while
    // the push animation is still running), which would otherwise stack a duplicate entry.
    val openSettings = {
        if (backStack.lastOrNull() != Screen.Settings) backStack.add(Screen.Settings)
    }
    val openProfile = { userId: String ->
        val profile = Screen.Profile(userId)
        if (backStack.lastOrNull() != profile) backStack.add(profile)
    }
    val goBack: () -> Unit = { backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        onBack = goBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = { pushTransition() },
        popTransitionSpec = { popTransition() },
        predictivePopTransitionSpec = { popTransition() },
        modifier = Modifier.fillMaxSize(),
        entryProvider = entryProvider {
            entry<Screen.Shell> {
                HomeNavShell(
                    onOpenSettings = openSettings,
                    onOpenProfile = openProfile,
                )
            }
            entry<Screen.Profile> { profile ->
                ProfileScreen(
                    userId = profile.userId,
                    onOpenSettings = openSettings,
                    onBack = goBack,
                )
            }
            entry<Screen.Settings> {
                SettingsScreen(onBack = goBack)
            }
        },
    )
}

/**
 * The tabbed shell: [DividedNavigationSuiteScaffold] adapts the navigation affordance to the
 * window size — bottom bar on phones, navigation rail on larger or unfolded screens — and the
 * selected [AppDestinations] entry chooses which screen fills the content area. Tabs are peer
 * destinations held as plain selection state (switching tabs is not a navigation event and
 * never grows the back stack), so the content cross-fades (fade-through) rather than sliding.
 */
@Composable
private fun HomeNavShell(
    onOpenSettings: () -> Unit,
    onOpenProfile: (userId: String) -> Unit,
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val navItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = LocalMosaicColors.current.textTertiary,
        ),
    )
    DividedNavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            painterResource(destination.icon),
                            contentDescription = stringResource(destination.labelRes),
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination },
                    colors = navItemColors,
                )
            }
        },
    ) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                (fadeIn(tween(durationMillis = 210, delayMillis = 90)) +
                    scaleIn(tween(durationMillis = 210, delayMillis = 90), initialScale = 0.94f)) togetherWith
                    fadeOut(tween(durationMillis = 90))
            },
            modifier = Modifier.fillMaxSize(),
            label = "tab",
        ) { destination ->
            when (destination) {
                AppDestinations.HOME -> HomeScreen(
                    onOpenSettings = onOpenSettings,
                    onOpenProfile = onOpenProfile,
                )

                AppDestinations.FAVORITES, AppDestinations.PROFILE ->
                    PlaceholderScreen(
                        titleRes = destination.labelRes,
                        onOpenSettings = onOpenSettings,
                    )
            }
        }
    }
}

/** The top-level tabs of the navigation suite; add or change a tab by editing the enum. */
enum class AppDestinations(
    @get:StringRes val labelRes: Int,
    @get:DrawableRes val icon: Int,
) {
    HOME(R.string.nav_home, R.drawable.ic_home),
    FAVORITES(R.string.nav_favorites, R.drawable.ic_favorite),
    PROFILE(R.string.nav_profile, R.drawable.ic_account_box),
}

/** How long a push or pop between full-screen pages runs. */
private const val PushPopDurationMillis = 320

/** Push: the new page slides in from the right while the old one recedes a quarter-width. */
private fun pushTransition(): ContentTransform {
    val duration = PushPopDurationMillis
    return (slideInHorizontally(tween(duration)) { width -> width } + fadeIn(tween(duration))) togetherWith
        (slideOutHorizontally(tween(duration)) { width -> -width / 4 } + fadeOut(tween(duration)))
}

/** Pop: the inverse of [pushTransition] — the leaving page slides back off to the right. */
private fun popTransition(): ContentTransform {
    val duration = PushPopDurationMillis
    return (slideInHorizontally(tween(duration)) { width -> -width / 4 } + fadeIn(tween(duration))) togetherWith
        (slideOutHorizontally(tween(duration)) { width -> width } + fadeOut(tween(duration)))
}
