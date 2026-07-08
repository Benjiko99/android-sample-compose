package uno.lux.sample.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads and applies the app's language. Unlike [SettingsRepository] the value is not ours to
 * store: the per-app language is platform state, so [AppCompatLocaleRepository] leaves persistence
 * to the platform and this seam only exposes it. [InMemoryAppLocaleRepository] is the in-memory
 * double for tests and previews.
 *
 * [setLanguage] is not `suspend` because applying a language is a synchronous hand-off — the
 * platform persists in the background and recreates the activity on the main thread.
 */
interface AppLocaleRepository {
    val language: Flow<AppLanguage>
    fun setLanguage(language: AppLanguage)
}

class InMemoryAppLocaleRepository(
    initialLanguage: AppLanguage = AppLanguage.SYSTEM,
) : AppLocaleRepository {

    private val state = MutableStateFlow(initialLanguage)
    override val language: Flow<AppLanguage> = state.asStateFlow()

    override fun setLanguage(language: AppLanguage) {
        state.value = language
    }
}
