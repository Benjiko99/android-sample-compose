package uno.lux.sample.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * [SettingsRepository] backed by a Preferences [DataStore], so the theme choice survives
 * process death and restarts. The store is a constructor dependency: production supplies the
 * app's store (with a one-time migration from the legacy SharedPreferences file), tests a
 * store over a temp file — which keeps this class plain-JVM testable, unlike its
 * SharedPreferences predecessor.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = dataStore.data
        // A failed disk read shouldn't crash collectors; treat it as "nothing persisted".
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences -> ThemeMode.fromName(preferences[KEY_THEME_MODE]) }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[KEY_THEME_MODE] = mode.name }
    }

    private companion object {
        // Also the key the SharedPreferences migration carries values over from — don't rename.
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
