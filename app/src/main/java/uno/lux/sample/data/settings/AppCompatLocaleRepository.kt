package uno.lux.sample.data.settings

import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [AppLocaleRepository] backed by the per-app language APIs — the platform's own store, so this
 * class persists nothing. `AppCompatDelegate` forwards to the framework's `LocaleManager` on
 * Android 13+ and, below that, keeps the choice itself (enabled by the `autoStoreLocales` flag on
 * the `AppLocalesMetadataHolderService` declared in the manifest) and re-applies it on the next
 * cold start. Applying a language recreates the activity, which is what re-reads the resources.
 *
 * Both `AppCompatDelegate` locale calls need the delegate to exist, so this must not be constructed
 * before an `AppCompatActivity` has attached — `MainActivity` injects it, which is early enough.
 *
 * The current value is mirrored into a [MutableStateFlow] because the delegate offers a getter,
 * not a stream; seeding it at construction picks up the language a previous launch persisted.
 */
class AppCompatLocaleRepository : AppLocaleRepository {

    private val state = MutableStateFlow(storedLanguage() ?: AppLanguage.Default)

    override val language: StateFlow<AppLanguage> = state.asStateFlow()

    override fun resolveInitialLanguage() {
        if (storedLanguage() != null) return

        setLanguage(AppLanguage.fromLanguageTags(systemLanguageTags()) ?: AppLanguage.Default)
    }

    override fun setLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.languageTag),
        )
        state.value = language
    }

    /** The language a previous launch pinned, or `null` while the per-app locale list is empty. */
    private fun storedLanguage(): AppLanguage? =
        AppLanguage.fromLanguageTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())

    /**
     * The device's preferred languages, most-preferred first. Read off the *system* resources
     * rather than the app's, which the per-app locale has already overridden by this point.
     */
    private fun systemLanguageTags(): String =
        ConfigurationCompat.getLocales(Resources.getSystem().configuration).toLanguageTags()
}
