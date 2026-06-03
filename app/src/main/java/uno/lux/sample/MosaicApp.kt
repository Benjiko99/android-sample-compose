package uno.lux.sample

import android.app.Application
import uno.lux.sample.data.SettingsRepository
import uno.lux.sample.data.SharedPreferencesSettingsRepository

/**
 * Owns app-wide singletons. The theme preference must be shared between the settings screen
 * (which writes it) and the app root (which applies it), so its repository lives here rather
 * than being created per-ViewModel. A DI framework (Hilt) would eventually replace this.
 */
class MosaicApp : Application() {
    val settingsRepository: SettingsRepository by lazy {
        SharedPreferencesSettingsRepository(getSharedPreferences("settings", MODE_PRIVATE))
    }
}
