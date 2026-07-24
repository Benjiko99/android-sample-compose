package uno.lux.sample.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uno.lux.sample.app.navigation.Navigator
import uno.lux.sample.app.util.stateInWhileSubscribed
import uno.lux.sample.settings.AppLanguage
import uno.lux.sample.settings.ThemeMode
import uno.lux.sample.settings.data.AppLocaleRepository
import uno.lux.sample.settings.data.DEFAULT_AUTO_PLAY_VIDEOS
import uno.lux.sample.settings.data.SettingsRepository
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
) : ViewModel(),
    SettingsActions {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateInWhileSubscribed(viewModelScope, ThemeMode.SYSTEM)

    val autoPlayVideos: StateFlow<Boolean> = settingsRepository.autoPlayVideos
        .stateInWhileSubscribed(viewModelScope, DEFAULT_AUTO_PLAY_VIDEOS)

    /** Already hot state on the repository — a language is always in effect — so it passes straight through. */
    val language: StateFlow<AppLanguage> = appLocaleRepository.language

    override fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    override fun setAutoPlayVideos(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPlayVideos(enabled) }
    }

    override fun setLanguage(language: AppLanguage) = appLocaleRepository.setLanguage(language)

    override fun goBack() = navigator.goBack()
}
