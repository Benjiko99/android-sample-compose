package uno.lux.sample.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores user settings. Exposes [themeMode] and [autoPlayVideos] reactively and updates them on
 * request.
 *
 * [DataStoreSettingsRepository] is the persistent production implementation;
 * [InMemorySettingsRepository] is the in-memory double for tests and previews.
 */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>

    /** Whether the feed starts a video on its own as it scrolls into view. On unless turned off. */
    val autoPlayVideos: Flow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAutoPlayVideos(enabled: Boolean)
}

class InMemorySettingsRepository(
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
    initialAutoPlayVideos: Boolean = true,
) : SettingsRepository {

    private val theme = MutableStateFlow(initialThemeMode)
    override val themeMode: Flow<ThemeMode> = theme.asStateFlow()

    private val autoPlay = MutableStateFlow(initialAutoPlayVideos)
    override val autoPlayVideos: Flow<Boolean> = autoPlay.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        theme.value = mode
    }

    override suspend fun setAutoPlayVideos(enabled: Boolean) {
        autoPlay.value = enabled
    }
}
