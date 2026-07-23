package uno.lux.sample.settings.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import uno.lux.sample.settings.ThemeMode

/**
 * Video auto-play is on until the user turns it off. Every layer that has to name that default —
 * the persisted flow when the key is absent, the in-memory double, and the `StateFlow` each
 * ViewModel starts from before the stored choice arrives — reads it from here, so the default is
 * one edit rather than four literals that have to agree.
 */
const val DefaultAutoPlayVideos = true

/**
 * Stores user settings. Exposes [themeMode] and [autoPlayVideos] reactively and updates them on
 * request.
 *
 * [DataStoreSettingsRepository] is the persistent production implementation;
 * [InMemorySettingsRepository] is the in-memory double for tests and previews.
 */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>

    /** Whether the feed starts a video on its own as it scrolls into view. [DefaultAutoPlayVideos] unless set. */
    val autoPlayVideos: Flow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAutoPlayVideos(enabled: Boolean)
}

class InMemorySettingsRepository(
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
    initialAutoPlayVideos: Boolean = DefaultAutoPlayVideos,
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
