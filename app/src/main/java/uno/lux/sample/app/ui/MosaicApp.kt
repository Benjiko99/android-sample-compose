package uno.lux.sample.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import uno.lux.sample.album.ui.AlbumViewerScreen
import uno.lux.sample.app.navigation.BackStackEntry
import uno.lux.sample.app.navigation.Navigator
import uno.lux.sample.app.navigation.Screen
import uno.lux.sample.app.navigation.backStackEntryProvider
import uno.lux.sample.app.navigation.rememberBackStack
import uno.lux.sample.app.util.findActivity
import uno.lux.sample.composer.ui.CreatePostScreen
import uno.lux.sample.post.ui.PostDetailScreen
import uno.lux.sample.profile.ui.ProfileScreen
import uno.lux.sample.settings.ui.SettingsScreen
import uno.lux.sample.user.data.domain.UserId
import uno.lux.sample.user.ui.EditProfileScreen
import uno.lux.sample.video.ui.FullscreenVideoScreen
import uno.lux.sample.video.ui.LocalVideoPlayback
import uno.lux.sample.video.ui.VideoPlaybackViewModel

/**
 * App shell. A Navigation 3 [NavDisplay] renders the top of a [rememberBackStack] back
 * stack of [BackStackEntry] keys: the tabbed [AppShell] is the permanent root, and a user's
 * [ProfileScreen] or the [SettingsScreen] push over it as full-screen pages (settings stacks
 * over a profile, too). Pushes slide in from the right and pops slide back off — the
 * predictive back gesture included — while tab switches inside the shell stay a fade-through
 * between peers. The entry decorators give every page its own saveable-state and ViewModel
 * store, so a pushed page's ViewModel is created on push and cleared on pop. Those stores are
 * scoped by [BackStackEntry.id], so *each push* is its own page: opening the same post twice
 * gives the second copy its own ViewModel and its own comment draft.
 *
 * Navigation itself is driven by the ViewModels: the composition keeps owning the back stack
 * (that's what keeps it savable across configuration changes and process death) but attaches
 * it to the injected [navigator], and each screen's ViewModel pushes and pops through that
 * seam instead of the host threading navigation lambdas down to every screen. Every entry —
 * the fullscreen viewers and the composer tab included — reaches the stack through its own
 * ViewModel, so this shell wires no navigation lambdas at all; it only builds the [Screen] keys.
 */
@Composable
fun MosaicApp(currentUserId: UserId, navigator: Navigator) {
    val backStack = rememberBackStack(navigator, root = Screen.Shell)

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
            entryProvider = backStackEntryProvider { screen ->
                when (screen) {
                    Screen.Shell -> {
                        AppShell(currentUserId = currentUserId)
                    }

                    is Screen.Profile -> {
                        ProfileScreen(userId = screen.userId, showBackButton = true)
                    }

                    Screen.Settings -> {
                        SettingsScreen()
                    }

                    Screen.EditProfile -> {
                        EditProfileScreen()
                    }

                    Screen.CreatePost -> {
                        CreatePostScreen()
                    }

                    is Screen.FullscreenVideo -> {
                        FullscreenVideoScreen(url = screen.url, title = screen.title)
                    }

                    is Screen.PostDetail -> {
                        PostDetailScreen(postId = screen.postId)
                    }

                    is Screen.AlbumViewer -> {
                        AlbumViewerScreen(
                            imageUrls = screen.images,
                            initialIndex = screen.initialIndex,
                        )
                    }
                }
            },
        )
    }
}

/** How long a push or pop between full-screen pages runs. */
private const val PUSH_POP_DURATION_MILLIS = 320

/** Push: the new page slides in from the right while the old one recedes a quarter-width. */
private fun pushTransition(): ContentTransform {
    val duration = PUSH_POP_DURATION_MILLIS

    return (
        slideInHorizontally(tween(duration)) { width ->
            width
        } + fadeIn(tween(duration))
    ) togetherWith
        (slideOutHorizontally(tween(duration)) { width -> -width / 4 } + fadeOut(tween(duration)))
}

/** Pop: the inverse of [pushTransition] — the leaving page slides back off to the right. */
private fun popTransition(): ContentTransform {
    val duration = PUSH_POP_DURATION_MILLIS

    return (
        slideInHorizontally(tween(duration)) { width ->
            -width / 4
        } + fadeIn(tween(duration))
    ) togetherWith
        (slideOutHorizontally(tween(duration)) { width -> width } + fadeOut(tween(duration)))
}
