package uno.lux.sample.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.navigation.Screen
import javax.inject.Inject

/**
 * Backs the stand-in tab screen. It carries no content state — only the one navigation intent
 * its top bar exposes, opening settings — which it routes through the injected [Navigator] like
 * every other screen. Settings is [goToSingleTop][Navigator.goToSingleTop] so tapping the gear
 * while settings is already open never stacks a second copy.
 */
@HiltViewModel
class PlaceholderViewModel @Inject constructor(
    private val navigator: Navigator,
) : ViewModel() {

    fun openSettings() = navigator.goToSingleTop(Screen.Settings)
}
