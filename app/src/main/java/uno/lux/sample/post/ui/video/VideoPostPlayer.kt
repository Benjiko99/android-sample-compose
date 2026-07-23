package uno.lux.sample.post.ui.video

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import uno.lux.sample.R
import uno.lux.sample.post.Video
import uno.lux.sample.design.components.MediaBadge
import uno.lux.sample.design.components.MosaicGradients
import uno.lux.sample.design.components.PlayBadge
import uno.lux.sample.design.components.debouncedClickable
import uno.lux.sample.util.formatVideoDuration

/**
 * A video post's media area: a 16:9 stage that starts as a tappable thumbnail and, once playing,
 * hosts the shared player inline. Playback is owned by [LocalVideoPlayback] — the same instance
 * the full-screen page reuses — so tapping the player's fullscreen control hands the running
 * stream to [onOpenFullscreen] without losing position. When the local is null (previews), only
 * the thumbnail renders.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun VideoPostPlayer(
    video: Video,
    onOpenFullscreen: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playback = LocalVideoPlayback.current
    val isActive = playback != null && playback.activeVideoUrl == video.videoUrl && playback.player != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .background(MosaicGradients.mediaBrush(video.id)),
        contentAlignment = Alignment.Center,
    ) {
        // `isActive` already establishes playback != null, so it smart-casts inside this branch.
        if (isActive) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                        setBackgroundColor(android.graphics.Color.BLACK)
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        setShowRewindButton(false)
                        setShowFastForwardButton(false)
                        contentDescription = video.title
                        setFullscreenButtonClickListener {
                            // Detach inline first so only the full-screen surface owns the player.
                            playback.enterFullscreen()
                            onOpenFullscreen(video)
                        }
                    }
                },
                // While full screen owns the surface the inline view detaches (player = null) but
                // never releases — the controller keeps the instance alive across the transition.
                update = { view -> view.player = if (playback.isFullscreen) null else playback.player },
                onRelease = { view -> view.player = null },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            VideoThumbnail(
                video = video,
                onPlay = { playback?.playInline(video.videoUrl) },
            )
        }
    }
}

/** The pre-playback poster: a centered play button over the gradient, with a duration badge. */
@Composable
private fun VideoThumbnail(
    video: Video,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .debouncedClickable(onClick = onPlay),
        contentAlignment = Alignment.Center,
    ) {
        PlayBadge(
            contentDescription = stringResource(R.string.video_play),
            size = 58.dp,
            iconSize = 32.dp,
        )
        MediaBadge(
            text = formatVideoDuration(video.durationSeconds),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
        )
    }
}
