package uno.lux.sample.post.ui.video

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import uno.lux.sample.design.components.ImmersiveSystemBars
import uno.lux.sample.design.components.OverlayBackButton
import uno.lux.sample.util.findActivity

/**
 * Stateful entry point: binds the [FullscreenVideoViewModel] (its sole job is routing back
 * through the `Navigator`) and forwards to the stateless overload below.
 */
@Composable
fun FullscreenVideoScreen(
    url: String,
    title: String?,
    modifier: Modifier = Modifier,
    viewModel: FullscreenVideoViewModel = hiltViewModel(),
) {
    FullscreenVideoScreen(
        url = url,
        title = title,
        onBack = viewModel::goBack,
        modifier = modifier,
    )
}

/**
 * The full-screen video page, pushed over the rest of the app. It does not own a player: it
 * reuses the shared one from [LocalVideoPlayback], so a video promoted from an inline post keeps
 * playing from the same position. [openFullscreen] either adopts the running inline player or, if
 * this page is the entry point (a profile thumbnail), loads the video fresh; the matching teardown
 * runs in [exitFullscreen] when the page is really popped. The system bars hide for an immersive
 * stage and are restored on the way out. Holding no ViewModel makes it directly previewable.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun FullscreenVideoScreen(
    url: String,
    title: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playback = LocalVideoPlayback.current

    LaunchedEffect(playback, url) {
        playback?.openFullscreen(url)
    }

    // Exit only on a real pop, not a configuration change — a rotation must keep playing.
    val activity = LocalContext.current.findActivity()

    DisposableEffect(playback) {
        onDispose {
            if (activity?.isChangingConfigurations != true) playback?.exitFullscreen()
        }
    }

    ImmersiveSystemBars()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    contentDescription = title
                    // The control already reads as "exit full screen"; tapping it pops the page.
                    setFullscreenButtonState(true)
                    setFullscreenButtonClickListener { onBack() }
                }
            },
            update = { view -> view.player = playback?.player },
            onRelease = { view -> view.player = null },
            modifier = Modifier.fillMaxSize(),
        )

        OverlayBackButton(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))
    }
}
