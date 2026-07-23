package uno.lux.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import uno.lux.sample.data.settings.AppLocaleRepository
import uno.lux.sample.data.settings.SettingsRepository
import uno.lux.sample.data.settings.ThemeMode
import uno.lux.sample.data.user.UserId
import uno.lux.sample.di.CurrentUserId
import uno.lux.sample.util.stateInWhileSubscribed
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
