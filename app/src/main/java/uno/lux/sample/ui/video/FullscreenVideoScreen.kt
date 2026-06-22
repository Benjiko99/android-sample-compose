package uno.lux.sample.ui.video

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import uno.lux.sample.R
import uno.lux.sample.ui.components.rememberDebounced

/**
 * The full-screen video page, pushed over the rest of the app. It does not own a player: it
 * reuses the shared one from [LocalVideoPlayback], so a video promoted from an inline post keeps
 * playing from the same position. [openFullscreen] either adopts the running inline player or, if
 * this page is the entry point (a profile thumbnail), loads the video fresh; the matching teardown
 * runs in [exitFullscreen] when the page is really popped. The system bars hide for an immersive
 * stage and are restored on the way out.
 */
@OptIn(UnstableApi::class)
@Composable
fun FullscreenVideoScreen(
    videoId: String,
    url: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playback = LocalVideoPlayback.current
    LaunchedEffect(playback, videoId, url) {
        playback?.openFullscreen(videoId, url)
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
        IconButton(
            onClick = onBack.rememberDebounced(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .safeDrawingPadding()
                .padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.navigate_back),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** Hides the system bars (swipe to reveal transiently) while shown, restoring them on dispose. */
@Composable
private fun ImmersiveSystemBars() {
    val view = LocalView.current
    val window = LocalContext.current.findActivity()?.window
    DisposableEffect(window) {
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
