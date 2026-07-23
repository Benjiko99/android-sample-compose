package uno.lux.sample.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import uno.lux.sample.settings.ThemeMode

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

    // A failed disk read shouldn't crash collectors; treat it as "nothing persisted".
    private val preferences: Flow<Preferences> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    override val themeMode: Flow<ThemeMode> = preferences
        .map { ThemeMode.fromName(it[KEY_THEME_MODE]) }

    // Absent means the user never opted out, which is the default — not `false`.
    override val autoPlayVideos: Flow<Boolean> = preferences
        .map { it[KEY_AUTO_PLAY_VIDEOS] ?: DefaultAutoPlayVideos }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[KEY_THEME_MODE] = mode.name }
    }

    override suspend fun setAutoPlayVideos(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_AUTO_PLAY_VIDEOS] = enabled }
    }

    private companion object {
        // Also the key the SharedPreferences migration carries values over from — don't rename.
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_AUTO_PLAY_VIDEOS = booleanPreferencesKey("auto_play_videos")
    }
}
