package uno.lux.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import uno.lux.sample.data.SettingsRepository
import uno.lux.sample.data.ThemeMode
import uno.lux.sample.util.stateInWhileSubscribed
import javax.inject.Inject

/**
 * State for the app root: the [themeMode] preference [MainActivity] resolves into the
 * light/dark choice. Kept in a ViewModel so the value survives configuration changes and the
 * activity stays a thin shell around [SampleApp].
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateInWhileSubscribed(viewModelScope, ThemeMode.SYSTEM)
}
