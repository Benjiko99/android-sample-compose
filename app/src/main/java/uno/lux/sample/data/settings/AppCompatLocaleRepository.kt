package uno.lux.sample.data.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [AppLocaleRepository] backed by the per-app language APIs — the platform's own store, so this
 * class persists nothing. `AppCompatDelegate` forwards to the framework's `LocaleManager` on
 * Android 13+ and, below that, keeps the choice itself (enabled by the `autoStoreLocales` flag on
 * the `AppLocalesMetadataHolderService` declared in the manifest) and re-applies it on the next
 * cold start. Applying a language recreates the activity, which is what re-reads the resources.
 *
 * The current value is mirrored into a [MutableStateFlow] because the delegate offers a getter,
 * not a stream; seeding it at construction picks up the language a previous launch persisted.
 */
class AppCompatLocaleRepository : AppLocaleRepository {

    private val state = MutableStateFlow(currentLanguage())
    override val language: Flow<AppLanguage> = state.asStateFlow()

    override fun setLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(language.toLocaleList())
        state.value = language
    }

    private fun currentLanguage(): AppLanguage =
        AppLanguage.fromLanguageTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())

    /** [AppLanguage.SYSTEM] has no tag, and an empty locale list is how "follow the system" reads. */
    private fun AppLanguage.toLocaleList(): LocaleListCompat =
        languageTag?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList()
}
