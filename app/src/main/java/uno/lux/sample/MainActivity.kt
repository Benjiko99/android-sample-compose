package uno.lux.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uno.lux.sample.data.ThemeMode
import uno.lux.sample.ui.PlaceholderScreen
import uno.lux.sample.ui.home.HomeScreen
import uno.lux.sample.ui.settings.SettingsScreen
import uno.lux.sample.ui.theme.SampleTheme

// Scrims drawn behind the gesture navigation bar, per the AndroidX edge-to-edge guidance.
private val LightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = (application as SampleApplication).settingsRepository
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
 * App shell. [NavigationSuiteScaffold] adapts the navigation affordance to the window size
 * — bottom bar on phones, navigation rail or drawer on larger or unfolded screens — while
 * the selected [AppDestinations] entry chooses which screen fills the content area. Settings
 * isn't a tab: each screen opens it from a top-bar action as a full-screen page, with system
 * back returning to the shell.
 */
@PreviewScreenSizes
@Composable
fun SampleApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(onBack = { showSettings = false })
    } else {
        NavigationSuiteScaffold(
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
                    )
                }
            },
        ) {
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(onOpenSettings = { showSettings = true })
                AppDestinations.FAVORITES, AppDestinations.PROFILE ->
                    PlaceholderScreen(
                        titleRes = currentDestination.labelRes,
                        onOpenSettings = { showSettings = true },
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
