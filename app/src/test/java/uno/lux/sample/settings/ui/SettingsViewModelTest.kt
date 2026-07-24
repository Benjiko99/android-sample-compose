package uno.lux.sample.settings.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.testing.ViewModelTest
import uno.lux.sample.settings.AppLanguage
import uno.lux.sample.settings.data.InMemoryAppLocaleRepository
import uno.lux.sample.settings.data.InMemorySettingsRepository
import uno.lux.sample.settings.ThemeMode
import uno.lux.sample.app.navigation.Navigator
import uno.lux.sample.app.navigation.Screen

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest : ViewModelTest() {

    private val backStack = mutableListOf<NavKey>(Screen.Shell, Screen.Settings)
    private val navigator = Navigator().apply { attach(backStack) }

    private fun viewModel(
        repository: InMemorySettingsRepository = InMemorySettingsRepository(),
        localeRepository: InMemoryAppLocaleRepository = InMemoryAppLocaleRepository(),
    ) = SettingsViewModel(repository, localeRepository, navigator)

    @Test
    fun `themeMode reflects the repository`() = runTest {
        val viewModel = viewModel(InMemorySettingsRepository(ThemeMode.DARK))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.themeMode.collect {}
        }

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun `setThemeMode updates the exposed theme`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.themeMode.collect {}
        }

        viewModel.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
    }

    @Test
    fun `autoPlayVideos reflects the repository`() = runTest {
        val viewModel = viewModel(InMemorySettingsRepository(initialAutoPlayVideos = true))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.autoPlayVideos.collect {}
        }

        assertEquals(true, viewModel.autoPlayVideos.value)
    }

    // Seeded opposite the default, so a ViewModel reading through to the repository instead of
    // starting from the shared default would fail here rather than agree by coincidence.
    @Test
    fun `autoPlayVideos is off before anything is collected`() {
        val repository = InMemorySettingsRepository(initialAutoPlayVideos = true)

        assertEquals(false, viewModel(repository).autoPlayVideos.value)
    }

    @Test
    fun `setAutoPlayVideos updates the exposed preference`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.autoPlayVideos.collect {}
        }

        viewModel.setAutoPlayVideos(false)

        assertEquals(false, viewModel.autoPlayVideos.value)
    }

    @Test
    fun `language reflects the repository`() {
        val viewModel = viewModel(localeRepository = InMemoryAppLocaleRepository(AppLanguage.CZECH))

        assertEquals(AppLanguage.CZECH, viewModel.language.value)
    }

    @Test
    fun `setLanguage updates the exposed language`() {
        val viewModel = viewModel()

        viewModel.setLanguage(AppLanguage.CZECH)

        assertEquals(AppLanguage.CZECH, viewModel.language.value)
    }

    @Test
    fun `goBack pops the settings page`() {
        viewModel().goBack()

        assertEquals(listOf<NavKey>(Screen.Shell), backStack)
    }
}
