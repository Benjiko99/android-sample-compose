package uno.lux.sample.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uno.lux.sample.SampleApplication
import uno.lux.sample.data.SettingsRepository
import uno.lux.sample.data.ThemeMode
import uno.lux.sample.util.stateInWhileSubscribed

/**
 * Exposes the current [themeMode] and applies the user's selection through the
 * [SettingsRepository]. [Factory] pulls the app-scoped repository off the [SampleApplication]
 * until a DI framework provides it.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateInWhileSubscribed(viewModelScope, ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as SampleApplication
                SettingsViewModel(application.settingsRepository)
            }
        }
    }
}
