package uno.lux.sample.settings.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.settings.ThemeMode

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

    @Test
    fun `auto-play is off by default`() = runTest {
        assertEquals(false, InMemorySettingsRepository().autoPlayVideos.first())
    }

    @Test
    fun `setAutoPlayVideos updates the exposed preference`() = runTest {
        val repository = InMemorySettingsRepository()

        repository.setAutoPlayVideos(false)

        assertEquals(false, repository.autoPlayVideos.first())
    }
}
