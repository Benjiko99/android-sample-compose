package uno.lux.sample.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.navigation.Screen
import javax.inject.Inject

/**
 * Backs the tabbed shell itself. The shell holds no content state — which tab is selected is
 * plain composition state, since switching tabs is not a navigation event — so its only intent is
 * the one navigation-suite item that *does* navigate: an [AppDestinations] entry carrying a
 * [Screen] opens that page over the whole shell instead of swapping the content area.
 *
 * The push is [goToSingleTop][Navigator.goToSingleTop] so hammering the Create item while the
 * composer is already open can never stack a second copy of it.
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val navigator: Navigator,
) : ViewModel() {

    fun openDestination(screen: Screen) = navigator.goToSingleTop(screen)
}
