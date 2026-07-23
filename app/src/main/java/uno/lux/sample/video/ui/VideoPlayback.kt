package uno.lux.sample.video.ui

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Owns the app's single video [ExoPlayer] so one playback can move between an inline post and
 * the full-screen page without being recreated — the position lives in the player, so reusing
 * the instance is what preserves it.
 *
 * Exactly one video plays at a time. [activeVideoUrl], [isFullscreen] and [player] are Compose
 * state, so the inline player and the full-screen page recompose as playback moves between them.
 * [ownedByInline] records whether an inline post started the playback: if so, leaving full
 * screen returns to that post and keeps playing; if a profile thumbnail opened it straight into
 * full screen, leaving releases it. The URL is the player's identity — every uploaded video has
 * its own, and a clip picked in the composer brings a content URI — so one keying serves posts
 * and local previews alike, with no id a local file wouldn't have.
 *
 * The player is an Android component with no logic to unit test, so this is a plain holder built
 * on [Context] rather than a constructor-injected, JVM-testable unit.
 */
@Stable
class VideoPlaybackController(private val appContext: Context) {

    /** The video currently loaded into [player], or null when nothing is playing. */
    var activeVideoUrl by mutableStateOf<String?>(null)
        private set

    /** True while playback is showing on the full-screen page rather than inline. */
    var isFullscreen by mutableStateOf(false)
        private set

    /** The shared player, or null when nothing is loaded. Attach it to a `PlayerView`. */
    var player by mutableStateOf<ExoPlayer?>(null)
        private set

    private var ownedByInline = false

    /** Starts (or resumes) inline playback of [url], the feed/profile-post entry point. */
    fun playInline(url: String) {
        ensurePlayer(url)
        ownedByInline = true
    }

    /** Promotes the already-playing inline video to full screen. */
    fun enterFullscreen() {
        isFullscreen = true
    }

    /**
     * Shows [url] on the full-screen page. If it is already the active video (an inline post
     * promoted it) the running player is reused untouched; otherwise it is loaded fresh with no
     * inline owner, so [exitFullscreen] will release it.
     */
    fun openFullscreen(url: String) {
        if (activeVideoUrl != url) {
            ensurePlayer(url)
            ownedByInline = false
        }
        isFullscreen = true
    }

    /** Leaves the full-screen page, releasing the player unless an inline post still owns it. */
    fun exitFullscreen() {
        isFullscreen = false
        if (!ownedByInline) stop()
    }

    /** Pauses playback without tearing the player down (e.g. when the app is backgrounded). */
    fun pause() {
        player?.pause()
    }

    /** Releases the player and clears all playback state. */
    fun stop() {
        player?.release()
        player = null
        activeVideoUrl = null
        ownedByInline = false
        isFullscreen = false
    }

    private fun ensurePlayer(url: String) {
        if (activeVideoUrl == url && player != null) return
        player?.release()
        player = ExoPlayer.Builder(appContext).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            playWhenReady = true
            prepare()
        }
        activeVideoUrl = url
    }
}

/**
 * Holds the [VideoPlaybackController] at activity scope so the shared player survives both the
 * navigation push to full screen and configuration changes, and is released in [onCleared] when
 * the activity is finished for good. Built on the application context, so it leaks nothing.
 */
@HiltViewModel
class VideoPlaybackViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {
    val controller = VideoPlaybackController(context)

    override fun onCleared() {
        controller.stop()
    }
}

/**
 * The shared [VideoPlaybackController] for the current composition. Null outside the app shell
 * (e.g. in `@Preview`s), where video controls render their static thumbnail state.
 */
val LocalVideoPlayback = compositionLocalOf<VideoPlaybackController?> { null }
