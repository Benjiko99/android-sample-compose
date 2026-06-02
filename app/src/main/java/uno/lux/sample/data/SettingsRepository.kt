package uno.lux.sample.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores user settings. Exposes [themeMode] reactively and updates it on request.
 *
 * Backed by in-memory state for now; the interface is the seam a DataStore-backed
 * implementation — which would persist the preference across restarts — slots into.
 */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

class InMemorySettingsRepository(
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
) : SettingsRepository {

    private val state = MutableStateFlow(initialThemeMode)
    override val themeMode: Flow<ThemeMode> = state.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = mode
    }
}
