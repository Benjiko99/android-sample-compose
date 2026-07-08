package uno.lux.sample.ui

import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import uno.lux.sample.R
import uno.lux.sample.ui.album.AlbumViewerScreen
import uno.lux.sample.ui.components.DividedNavigationSuiteScaffold
import uno.lux.sample.ui.editprofile.EditProfileScreen
import uno.lux.sample.ui.home.HomeScreen
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.navigation.Screen
import uno.lux.sample.ui.post.PostDetailScreen
import uno.lux.sample.ui.profile.ProfileScreen
import uno.lux.sample.ui.settings.SettingsScreen
import uno.lux.sample.ui.theme.LocalMosaicColors
import uno.lux.sample.ui.video.FullscreenVideoScreen
import uno.lux.sample.ui.video.LocalVideoPlayback
import uno.lux.sample.ui.video.VideoPlaybackViewModel
import uno.lux.sample.util.findActivity

/**
 * App shell. A Navigation 3 [NavDisplay] renders the top of a [rememberNavBackStack] back
 * stack of [Screen] keys: the tabbed [HomeNavShell] is the permanent root, and a user's
 * [ProfileScreen] or the [SettingsScreen] push over it as full-screen pages (settings stacks
 * over a profile, too). Pushes slide in from the right and pops slide back off — the
 * predictive back gesture included — while tab switches inside the shell stay a fade-through
 * between peers. The entry decorators give every page its own saveable-state and ViewModel
 * store, so a pushed page's ViewModel is created on push and cleared on pop.
 *
 * Navigation itself is driven by the ViewModels: the composition keeps owning the back stack
 * (that's what keeps it saveable across configuration changes and process death) but attaches
 * it to the injected [navigator], and each screen's ViewModel pushes and pops through that
 * seam instead of the host threading navigation lambdas down to every screen. Every entry —
 * the fullscreen viewers and the placeholder tab included — reaches the stack through its own
 * ViewModel, so this shell wires no navigation lambdas at all; it only builds the [Screen] keys.
 */
@Composable
fun SampleApp(currentUserId: String, navigator: Navigator) {
    val backStack = rememberNavBackStack(Screen.Shell)

    DisposableEffect(navigator, backStack) {
        navigator.attach(backStack)
        onDispose { navigator.detach(backStack) }
    }

    // The shared player lives in an activity-scoped ViewModel so it survives the push to full
    // screen and configuration changes, and is released when the activity finishes. Provided to
    // the whole back stack so inline posts and the full-screen page reach the same instance.
    val activity = LocalContext.current.findActivity() as ComponentActivity
    val playback = hiltViewModel<VideoPlaybackViewModel>(activity).controller

    // Pause playback when the app is backgrounded; it resumes on the user's next tap.
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, playback) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) playback.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CompositionLocalProvider(LocalVideoPlayback provides playback) {
        NavDisplay(
            backStack = backStack,
            onBack = navigator::goBack,
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
                    HomeNavShell(currentUserId = currentUserId)
                }
                entry<Screen.Profile> { profile ->
                    ProfileScreen(userId = profile.userId, showBackButton = true)
                }
                entry<Screen.Settings> {
                    SettingsScreen()
                }
                entry<Screen.EditProfile> {
                    EditProfileScreen()
                }
                entry<Screen.FullscreenVideo> { video ->
                    FullscreenVideoScreen(
                        videoId = video.videoId,
                        url = video.url,
                        title = video.title,
                    )
                }
                entry<Screen.PostDetail> { detail ->
                    PostDetailScreen(postId = detail.postId)
                }
                entry<Screen.AlbumViewer> { key ->
                    AlbumViewerScreen(
                        imageUrls = key.images,
                        initialIndex = key.initialIndex,
                    )
                }
            },
        )
    }
}

/**
 * The tabbed shell: [DividedNavigationSuiteScaffold] adapts the navigation affordance to the
 * window size — bottom bar on phones, navigation rail on larger or unfolded screens — and the
 * selected [AppDestinations] entry chooses which screen fills the content area. Tabs are peer
 * destinations held as plain selection state (switching tabs is not a navigation event and
 * never grows the back stack), so the content cross-fades (fade-through) rather than sliding.
 */
@Composable
private fun HomeNavShell(currentUserId: String) {
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
                    scaleIn(animationSpec = tween(durationMillis = 210, delayMillis = 90), initialScale = 0.94f)) togetherWith
                    fadeOut(tween(durationMillis = 90))
            },
            modifier = Modifier.fillMaxSize(),
            label = "tab",
        ) { destination ->
            when (destination) {
                AppDestinations.HOME -> HomeScreen()

                // The Profile tab is the signed-in user's own profile (no up-affordance, so
                // showBackButton stays false); tapping another author still pushes Screen.Profile.
                AppDestinations.PROFILE -> ProfileScreen(userId = currentUserId)

                AppDestinations.FAVORITES ->
                    PlaceholderScreen(titleRes = destination.labelRes)
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
