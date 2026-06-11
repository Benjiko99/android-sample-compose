package uno.lux.sample.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    /** A real Preferences DataStore over a per-test file, torn down with the test scope. */
    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
        ) { File(tempFolder.root, "settings.preferences_pb") }

    @Test
    fun `themeMode defaults to SYSTEM when nothing is persisted`() = runTest {
        val repository = DataStoreSettingsRepository(dataStore())

        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
    }

    @Test
    fun `setThemeMode persists the mode the flow then emits`() = runTest {
        val repository = DataStoreSettingsRepository(dataStore())

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }

    @Test
    fun `themeMode falls back to SYSTEM for an unrecognized persisted value`() = runTest {
        val dataStore = dataStore()
        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "SOLARIZED" }
        val repository = DataStoreSettingsRepository(dataStore)

        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
    }

    @Test
    fun `setThemeMode writes under the key the SharedPreferences migration fills`() = runTest {
        val dataStore = dataStore()
        val repository = DataStoreSettingsRepository(dataStore)

        repository.setThemeMode(ThemeMode.LIGHT)

        // "theme_mode" is the contract with the one-time SharedPreferences migration (the old
        // file used the same key), so a rename would silently drop every user's saved choice.
        assertEquals("LIGHT", dataStore.data.first()[stringPreferencesKey("theme_mode")])
    }
}
