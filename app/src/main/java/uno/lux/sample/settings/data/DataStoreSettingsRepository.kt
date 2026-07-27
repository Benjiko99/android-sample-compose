package uno.lux.sample.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uno.lux.sample.settings.data.domain.DEFAULT_AUTO_PLAY_VIDEOS
import uno.lux.sample.settings.data.domain.Settings
import uno.lux.sample.settings.data.domain.ThemeMode
import java.io.IOException

/**
 * [SettingsRepository] backed by a Preferences [DataStore], so the stored choices survive
 * process death and restarts. The store is a constructor dependency: production supplies the
 * app's store (with a one-time migration from the legacy SharedPreferences file), tests a
 * store over a temp file — which keeps this class plain-JVM testable, unlike its
 * SharedPreferences predecessor.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<Settings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[KEY_THEME_MODE] = mode.name }
    }

    override suspend fun setAutoPlayVideos(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_AUTO_PLAY_VIDEOS] = enabled }
    }

    private fun Preferences.toSettings() = Settings(
        themeMode = ThemeMode.fromName(this[KEY_THEME_MODE]),
        autoPlayVideos = this[KEY_AUTO_PLAY_VIDEOS] ?: DEFAULT_AUTO_PLAY_VIDEOS,
    )

    private companion object {
        // Also the key the SharedPreferences migration carries values over from — don't rename.
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_AUTO_PLAY_VIDEOS = booleanPreferencesKey("auto_play_videos")
    }
}
