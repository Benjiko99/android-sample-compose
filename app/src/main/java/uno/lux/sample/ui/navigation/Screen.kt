package uno.lux.sample.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The full-screen pages of the app — the keys Navigation 3's back stack is made of. [Shell]
 * is the permanent root carrying the navigation-suite tabs; [Profile], [Settings] and
 * [VideoPlayer] are pushed over it (settings can stack on top of a profile, too). Keys are
 * [Serializable] so `rememberNavBackStack` can persist the stack across configuration changes
 * and process death.
 */
@Serializable
sealed interface Screen : NavKey {

    /** The adaptive navigation shell hosting the top-level tabs; always the bottom entry. */
    @Serializable
    data object Shell : Screen

    /** A user's profile page, opened from a post's author header. */
    @Serializable
    data class Profile(val userId: String) : Screen

    /** The settings page, opened from the gear action any screen's top bar carries. */
    @Serializable
    data object Settings : Screen

    /** The full-screen video player, opened by tapping a video on a profile's Videos tab. */
    @Serializable
    data class VideoPlayer(val url: String, val title: String) : Screen
}
