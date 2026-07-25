package uno.lux.sample.settings.data

import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.settings.data.domain.AppLanguage

class InMemoryAppLocaleRepositoryTest {

    @Test
    fun `a stored language is exposed as-is`() {
        val repository = InMemoryAppLocaleRepository(storedLanguage = AppLanguage.CZECH)

        assertEquals(AppLanguage.CZECH, repository.language.value)
    }

    @Test
    fun `setLanguage emits the new language`() {
        val repository = InMemoryAppLocaleRepository(storedLanguage = AppLanguage.ENGLISH)

        repository.setLanguage(AppLanguage.CZECH)

        assertEquals(AppLanguage.CZECH, repository.language.value)
    }

    @Test
    fun `on first launch the device's language is adopted when we ship it`() {
        val repository = InMemoryAppLocaleRepository(systemLanguageTags = "cs-CZ")

        repository.resolveInitialLanguage()

        assertEquals(AppLanguage.CZECH, repository.language.value)
    }

    @Test
    fun `on first launch a device language we don't ship falls back to English`() {
        val repository = InMemoryAppLocaleRepository(systemLanguageTags = "de-DE,sk-SK")

        repository.resolveInitialLanguage()

        assertEquals(AppLanguage.ENGLISH, repository.language.value)
    }

    @Test
    fun `on first launch the best of the device's preferred languages wins`() {
        val repository = InMemoryAppLocaleRepository(systemLanguageTags = "de-DE,cs-CZ,en-US")

        repository.resolveInitialLanguage()

        assertEquals(AppLanguage.CZECH, repository.language.value)
    }

    @Test
    fun `resolveInitialLanguage leaves a stored language alone`() {
        val repository = InMemoryAppLocaleRepository(
            storedLanguage = AppLanguage.ENGLISH,
            systemLanguageTags = "cs-CZ",
        )

        repository.resolveInitialLanguage()

        assertEquals(AppLanguage.ENGLISH, repository.language.value)
    }

    @Test
    fun `resolveInitialLanguage pins its choice, so a later call cannot re-resolve it`() {
        val repository = InMemoryAppLocaleRepository(systemLanguageTags = "de-DE")

        repository.resolveInitialLanguage()
        repository.setLanguage(AppLanguage.CZECH)
        repository.resolveInitialLanguage()

        assertEquals(AppLanguage.CZECH, repository.language.value)
    }
}
