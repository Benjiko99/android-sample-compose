package uno.lux.sample.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import uno.lux.sample.R
import uno.lux.sample.data.post.Album
import uno.lux.sample.ui.components.rememberDebounced
import uno.lux.sample.ui.video.findActivity

/**
 * Full-screen album viewer: a black-background [HorizontalPager] that opens at [initialIndex]
 * and lets the user swipe between [album]'s images. Each page supports pinch-to-zoom — while
 * zoomed in, single-finger pan moves the image and the pager scroll is suppressed; at scale 1
 * single-finger swipes pass through to the pager as normal.
 */
@Composable
fun AlbumViewerScreen(
    album: Album,
    initialIndex: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { album.images.size }

    ImmersiveSystemBars()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ZoomableImage(
                url = album.images[page],
                contentDescription = album.title,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (album.images.size > 1) {
            PageIndicator(
                pagerState = pagerState,
                total = album.images.size,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(bottom = 20.dp),
            )
        }

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

/**
 * The "n / total" page counter pill. It reads [PagerState.currentPage] in its own scope, so
 * swiping between pages recomposes only this pill — not the pager, the zoom state, or the back
 * button — keeping the read off the screen's top-level scope.
 */
@Composable
private fun PageIndicator(
    pagerState: PagerState,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = "${pagerState.currentPage + 1} / $total",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

/**
 * An image that supports pinch-to-zoom (1×–5×). At scale = 1 single-finger events are NOT
 * consumed so the parent [HorizontalPager] can swipe normally. At scale > 1, or when two or
 * more fingers are down, position changes are consumed so the pager is locked.
 */
@Composable
private fun ZoomableImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount >= 2 || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid()
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            val dz = newScale / scale
                            // Anchor the zoom to the centroid so the point under the fingers
                            // stays fixed. Centroid is in composable coordinates; graphicsLayer
                            // pivots at the composable center, so adjust relative to that.
                            val c = centroid - Offset(size.width / 2f, size.height / 2f)
                            offset = offset * dz - c * (dz - 1f) + pan
                            scale = newScale
                            if (newScale <= 1f) offset = Offset.Zero
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
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
