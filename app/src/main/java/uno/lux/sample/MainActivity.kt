package uno.lux.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import uno.lux.sample.ui.SampleApp
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.theme.MosaicTheme
import javax.inject.Inject

// Scrims drawn behind the gesture navigation bar, per the AndroidX edge-to-edge guidance.
private val LightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** The retained navigator ViewModels navigate through; [SampleApp] attaches its back stack. */
    @Inject
    lateinit var navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.themeMode.value == null }
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val resolvedMode = themeMode ?: return@setContent
            val darkTheme = resolvedMode.isDark(isSystemInDarkTheme())

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

            MosaicTheme(darkTheme = darkTheme) {
                SampleApp(currentUserId = viewModel.currentUserId, navigator = navigator)
            }
        }
    }
}
