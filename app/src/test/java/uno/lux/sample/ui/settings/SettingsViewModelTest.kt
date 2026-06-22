package uno.lux.sample.ui.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import uno.lux.sample.MainDispatcherRule
import uno.lux.sample.data.settings.InMemorySettingsRepository
import uno.lux.sample.data.settings.ThemeMode

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `themeMode reflects the repository`() = runTest {
        val viewModel = SettingsViewModel(InMemorySettingsRepository(ThemeMode.DARK))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.themeMode.collect {}
        }

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun `setThemeMode updates the exposed theme`() = runTest {
        val viewModel = SettingsViewModel(InMemorySettingsRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.themeMode.collect {}
        }

        viewModel.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
    }
}
