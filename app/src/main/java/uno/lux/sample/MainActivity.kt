package uno.lux.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uno.lux.sample.data.ThemeMode
import uno.lux.sample.ui.PlaceholderScreen
import uno.lux.sample.ui.components.DividedNavigationSuiteScaffold
import uno.lux.sample.ui.home.HomeScreen
import uno.lux.sample.ui.profile.ProfileScreen
import uno.lux.sample.ui.settings.SettingsScreen
import uno.lux.sample.ui.theme.LocalMosaicColors
import uno.lux.sample.ui.theme.SampleTheme

// Scrims drawn behind the gesture navigation bar, per the AndroidX edge-to-edge guidance.
private val LightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = (application as MosaicApp).settingsRepository
            val themeMode by settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val darkTheme = themeMode.isDark(isSystemInDarkTheme())

            // Re-apply edge-to-edge styling whenever the resolved theme flips, so the system
            // bar icons keep the right contrast against the app background.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
                )
                onDispose {}
            }

            SampleTheme(darkTheme = darkTheme) {
                SampleApp()
            }
        }
    }
}

/**
 * App shell. The bottom-nav [HomeNavShell], the [SettingsScreen] and a user's [ProfileScreen]
 * are the three full-screen views; Settings and Profile aren't tabs but open as pages over the
 * shell (system back returns). [AnimatedContent] animates every switch — a directional push /
 * pop slide for the deeper pages (see [pushPopTransition]), a fade-through for the peer tabs
 * inside the shell. Back handling is hoisted here, reflecting the top view independent of the
 * in-flight animation.
 */
@PreviewScreenSizes
@Composable
fun SampleApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var profileUserId by rememberSaveable { mutableStateOf<String?>(null) }
    val openSettings = { showSettings = true }
    val closeSettings = { showSettings = false }
    val openProfile = { userId: String -> profileUserId = userId }
    val closeProfile = { profileUserId = null }
    val activeProfileId = profileUserId

    when {
        showSettings -> BackHandler(onBack = closeSettings)
        activeProfileId != null -> BackHandler(onBack = closeProfile)
    }

    val screen: Screen = when {
        showSettings -> Screen.Settings
        activeProfileId != null -> Screen.Profile(activeProfileId)
        else -> Screen.Shell
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { pushPopTransition(initialState, targetState) },
        modifier = Modifier.fillMaxSize(),
        label = "screen",
    ) { target ->
        when (target) {
            Screen.Shell -> HomeNavShell(
                currentDestination = currentDestination,
                onSelectDestination = { currentDestination = it },
                onOpenSettings = openSettings,
                onOpenProfile = openProfile,
            )

            Screen.Settings -> SettingsScreen(onBack = closeSettings)

            is Screen.Profile -> ProfileScreen(
                userId = target.userId,
                onOpenSettings = openSettings,
                onBack = closeProfile,
            )
        }
    }
}

/**
 * The bottom-nav shell: [DividedNavigationSuiteScaffold] adapts the navigation affordance to the
 * window size — bottom bar on phones, navigation rail on larger or unfolded screens — and the
 * selected [AppDestinations] entry chooses which screen fills the content area. Tabs are peer
 * destinations, so the content cross-fades (fade-through) rather than sliding.
 */
@Composable
private fun HomeNavShell(
    currentDestination: AppDestinations,
    onSelectDestination: (AppDestinations) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: (userId: String) -> Unit,
) {
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
                    onClick = { onSelectDestination(destination) },
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

enum class AppDestinations(
    @get:StringRes val labelRes: Int,
    @get:DrawableRes val icon: Int,
) {
    HOME(R.string.nav_home, R.drawable.ic_home),
    FAVORITES(R.string.nav_favorites, R.drawable.ic_favorite),
    PROFILE(R.string.nav_profile, R.drawable.ic_account_box),
}

/**
 * The three full-screen views, ordered by [depth] so a transition between any two knows its
 * direction: opening a deeper view pushes it in from the right, going back pops it off to the
 * right, and the higher-depth view always draws on top.
 */
private sealed interface Screen {
    val depth: Int

    data object Shell : Screen {
        override val depth = 0
    }

    data class Profile(val userId: String) : Screen {
        override val depth = 1
    }

    data object Settings : Screen {
        override val depth = 2
    }
}

/** A horizontal shared-axis push (deeper) / pop (back) between two [Screen]s. */
private fun pushPopTransition(from: Screen, to: Screen): ContentTransform {
    val duration = 320
    val forward = to.depth > from.depth
    val enter = if (forward) {
        slideInHorizontally(tween(duration)) { width -> width } + fadeIn(tween(duration))
    } else {
        slideInHorizontally(tween(duration)) { width -> -width / 4 } + fadeIn(tween(duration))
    }
    val exit = if (forward) {
        slideOutHorizontally(tween(duration)) { width -> -width / 4 } + fadeOut(tween(duration))
    } else {
        slideOutHorizontally(tween(duration)) { width -> width } + fadeOut(tween(duration))
    }
    // The view being entered sits at its own depth; the higher-depth view stays on top, so a
    // pop slides the leaving page off to reveal the one beneath.
    return (enter togetherWith exit).apply { targetContentZIndex = to.depth.toFloat() }
}
