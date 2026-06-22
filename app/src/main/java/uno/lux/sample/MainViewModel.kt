package uno.lux.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import uno.lux.sample.data.settings.SettingsRepository
import uno.lux.sample.data.settings.ThemeMode
import uno.lux.sample.di.CurrentUserId
import uno.lux.sample.util.stateInWhileSubscribed
import javax.inject.Inject

/**
 * State for the app root: the [themeMode] preference [MainActivity] resolves into the
 * light/dark choice, and the [currentUserId] of the signed-in user (used to open their own
 * profile from the Profile tab). Kept in a ViewModel so the values survive configuration
 * changes and the activity stays a thin shell around [SampleApp].
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    @CurrentUserId val currentUserId: String,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateInWhileSubscribed(viewModelScope, ThemeMode.SYSTEM)
}
