package uno.lux.sample.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import uno.lux.sample.app.di.CurrentUserId
import uno.lux.sample.app.util.stateInWhileSubscribed
import uno.lux.sample.settings.data.AppLocaleRepository
import uno.lux.sample.settings.data.SettingsRepository
import uno.lux.sample.settings.data.domain.ThemeMode
import uno.lux.sample.user.data.domain.UserId
import javax.inject.Inject

/**
 * Root state for the app.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val appLocaleRepository: AppLocaleRepository,
    @param:CurrentUserId val currentUserId: UserId,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode?> = settingsRepository.themeMode
        .stateInWhileSubscribed(viewModelScope, initialValue = null)

    /**
     * First launch only: adopts the device's language if we ship it. Called from
     * the Activity because the locale APIs need AppCompat's delegate to
     * have attached — which is only guaranteed once `super.onCreate` has run.
     */
    fun resolveInitialLanguage() {
        appLocaleRepository.resolveInitialLanguage()
    }
}
