package uno.lux.sample.app

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.app.ui.MainViewModel
import uno.lux.sample.settings.data.AppLocaleRepository
import uno.lux.sample.settings.data.InMemoryAppLocaleRepository
import uno.lux.sample.settings.data.InMemorySettingsRepository
import uno.lux.sample.settings.data.domain.AppLanguage
import uno.lux.sample.settings.data.domain.ThemeMode
import uno.lux.sample.testing.ViewModelTest

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest : ViewModelTest() {

    private fun viewModel(
        settingsRepository: InMemorySettingsRepository = InMemorySettingsRepository(ThemeMode.DARK),
        appLocaleRepository: AppLocaleRepository = InMemoryAppLocaleRepository(),
    ) = MainViewModel(settingsRepository, appLocaleRepository, currentUserId = "u1")

    @Test
    fun `themeMode is null until the flow is collected`() {
        val viewModel = viewModel()

        assertEquals(null, viewModel.themeMode.value)
    }

    @Test
    fun `themeMode reflects the repository`() = runTest {
        val repository = InMemorySettingsRepository(ThemeMode.DARK)
        val viewModel = viewModel(settingsRepository = repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.themeMode.collect {}
        }

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)

        repository.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
    }

    @Test
    fun `resolveInitialLanguage pins the device language on a first launch`() {
        val locales = InMemoryAppLocaleRepository(systemLanguageTags = "cs-CZ,en-US")
        val viewModel = viewModel(appLocaleRepository = locales)

        viewModel.resolveInitialLanguage()

        assertEquals(AppLanguage.CZECH, locales.language.value)
    }

    @Test
    fun `resolveInitialLanguage leaves a stored language alone`() {
        val locales = InMemoryAppLocaleRepository(
            storedLanguage = AppLanguage.ENGLISH,
            systemLanguageTags = "cs-CZ",
        )
        val viewModel = viewModel(appLocaleRepository = locales)

        viewModel.resolveInitialLanguage()

        assertEquals(AppLanguage.ENGLISH, locales.language.value)
    }
}
