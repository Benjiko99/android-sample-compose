package uno.lux.sample.data.settings

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemorySettingsRepositoryTest {

    @Test
    fun `defaults to the system theme`() = runTest {
        assertEquals(ThemeMode.SYSTEM, InMemorySettingsRepository().themeMode.first())
    }

    @Test
    fun `setThemeMode updates the exposed theme`() = runTest {
        val repository = InMemorySettingsRepository()

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }
}
