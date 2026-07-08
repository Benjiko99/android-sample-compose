package uno.lux.sample.data.settings

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryAppLocaleRepositoryTest {

    @Test
    fun `language starts at SYSTEM by default`() = runTest {
        val repository = InMemoryAppLocaleRepository()

        assertEquals(AppLanguage.SYSTEM, repository.language.first())
    }

    @Test
    fun `language starts at the seeded value`() = runTest {
        val repository = InMemoryAppLocaleRepository(AppLanguage.CZECH)

        assertEquals(AppLanguage.CZECH, repository.language.first())
    }

    @Test
    fun `setLanguage emits the new language`() = runTest {
        val repository = InMemoryAppLocaleRepository()

        repository.setLanguage(AppLanguage.CZECH)

        assertEquals(AppLanguage.CZECH, repository.language.first())
    }
}
