package uno.lux.sample

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.data.settings.InMemorySettingsRepository
import uno.lux.sample.data.settings.ThemeMode

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest : ViewModelTest() {

    @Test
    fun `themeMode defaults to SYSTEM until something collects it`() {
        val viewModel = MainViewModel(InMemorySettingsRepository(ThemeMode.DARK), currentUserId = "u1")

        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
    }

    @Test
    fun `themeMode reflects the repository`() = runTest {
        val repository = InMemorySettingsRepository(ThemeMode.DARK)
        val viewModel = MainViewModel(repository, currentUserId = "u1")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.themeMode.collect {}
        }

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)

        repository.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
    }
}
