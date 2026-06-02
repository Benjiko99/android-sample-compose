package uno.lux.sample.data

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * [SettingsRepository] backed by [SharedPreferences], so the theme preference survives
 * process death and restarts. Writes go through the editor; external changes are observed
 * with a preference-change listener and re-emitted on [themeMode].
 */
class SharedPreferencesSettingsRepository(
    private val preferences: SharedPreferences,
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = callbackFlow {
        fun currentThemeMode() = ThemeMode.fromName(preferences.getString(KEY_THEME_MODE, null))

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME_MODE || key == null) trySend(currentThemeMode())
        }
        trySend(currentThemeMode())
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        preferences.edit { putString(KEY_THEME_MODE, mode.name) }
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
