package uno.lux.sample.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads and applies the app's language. Unlike [SettingsRepository] the value is not ours to
 * store: the per-app language is platform state, so [AppCompatLocaleRepository] leaves persistence
 * to the platform and this seam only exposes it. [InMemoryAppLocaleRepository] is the in-memory
 * double for tests and previews.
 *
 * [language] is a [StateFlow] rather than a plain `Flow` because a language is always in effect —
 * there is no "not loaded yet" state for a reader to render around.
 *
 * [setLanguage] is not `suspend` because applying a language is a synchronous hand-off — the
 * platform persists in the background and recreates the activity on the main thread.
 */
interface AppLocaleRepository {

    val language: StateFlow<AppLanguage>

    /**
     * Pins a language on first launch, when nothing is stored yet: the best of the device's
     * preferred languages that we ship, or [AppLanguage.Default] when we ship none of them. A no-op
     * once a language is stored, so it is safe to call on every launch — and it must be, because
     * the stored language is what keeps the app steady if the device's language later changes.
     */
    fun resolveInitialLanguage()

    fun setLanguage(language: AppLanguage)
}

/**
 * @param storedLanguage the language a previous launch persisted, or `null` for a first launch.
 * @param systemLanguageTags stands in for the device's preferred languages, as a BCP 47 tag list.
 */
class InMemoryAppLocaleRepository(
    storedLanguage: AppLanguage? = null,
    private val systemLanguageTags: String = "",
) : AppLocaleRepository {

    private var stored: AppLanguage? = storedLanguage
    private val state = MutableStateFlow(storedLanguage ?: AppLanguage.Default)

    override val language: StateFlow<AppLanguage> = state.asStateFlow()

    override fun resolveInitialLanguage() {
        if (stored != null) return

        setLanguage(AppLanguage.fromLanguageTags(systemLanguageTags) ?: AppLanguage.Default)
    }

    override fun setLanguage(language: AppLanguage) {
        stored = language
        state.value = language
    }
}
