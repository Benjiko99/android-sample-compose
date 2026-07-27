package uno.lux.sample.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import uno.lux.sample.app.navigation.Navigator
import uno.lux.sample.app.util.stateInWhileSubscribed
import uno.lux.sample.settings.data.AppLocaleRepository
import uno.lux.sample.settings.data.SettingsRepository
import uno.lux.sample.settings.data.domain.AppLanguage
import uno.lux.sample.settings.data.domain.ThemeMode
import javax.inject.Inject

/**
 * Exposes the current [themeMode], [autoPlayVideos] and [language] and applies the user's selection
 * through the [SettingsRepository] and [AppLocaleRepository]; the up button pops the page through
 * the injected [Navigator]. All three are constructor dependencies so the ViewModel can be unit
 * tested against fake repositories and a navigator attached to a plain list.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appLocaleRepository: AppLocaleRepository,
    private val navigator: Navigator,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeMode,
        settingsRepository.autoPlayVideos,
        appLocaleRepository.language,
    ) { themeMode, autoPlayVideos, language ->
        SettingsUiState.Content(
            themeMode = themeMode,
            autoPlayVideos = autoPlayVideos,
            language = language,
        )
    }.stateInWhileSubscribed(viewModelScope, SettingsUiState.Loading)

    fun eventSink(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SetThemeMode -> {
                setThemeMode(event.mode)
            }

            is SettingsUiEvent.SetAutoPlayVideos -> {
                setAutoPlayVideos(event.enabled)
            }

            is SettingsUiEvent.SetLanguage -> {
                setLanguage(event.language)
            }

            SettingsUiEvent.GoBack -> {
                navigator.goBack()
            }
        }
    }

    private fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    private fun setAutoPlayVideos(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPlayVideos(enabled) }
    }

    private fun setLanguage(language: AppLanguage) = appLocaleRepository.setLanguage(language)
}
