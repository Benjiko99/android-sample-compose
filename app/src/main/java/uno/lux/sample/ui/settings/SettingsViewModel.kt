package uno.lux.sample.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uno.lux.sample.data.settings.SettingsRepository
import uno.lux.sample.data.settings.ThemeMode
import uno.lux.sample.util.stateInWhileSubscribed
import javax.inject.Inject

/**
 * Exposes the current [themeMode] and applies the user's selection through the
 * [SettingsRepository]. The repository is a constructor dependency so the ViewModel can be
 * unit tested against a fake; in production Hilt injects the app-wide singleton.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel(), SettingsActions {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateInWhileSubscribed(viewModelScope, ThemeMode.SYSTEM)

    override fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }
}
