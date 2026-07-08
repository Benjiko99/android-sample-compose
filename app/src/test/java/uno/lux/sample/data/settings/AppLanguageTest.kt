package uno.lux.sample.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `a bare language tag resolves to its language`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTags("en"))
        assertEquals(AppLanguage.CZECH, AppLanguage.fromLanguageTags("cs"))
    }

    @Test
    fun `a region subtag is ignored`() {
        assertEquals(AppLanguage.CZECH, AppLanguage.fromLanguageTags("cs-CZ"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTags("en-GB"))
    }

    @Test
    fun `only the first tag of a list decides`() {
        assertEquals(AppLanguage.CZECH, AppLanguage.fromLanguageTags("cs-CZ,en-US"))
    }

    @Test
    fun `an uppercase tag resolves`() {
        assertEquals(AppLanguage.CZECH, AppLanguage.fromLanguageTags("CS"))
    }

    @Test
    fun `an empty, absent or unshipped language falls back to SYSTEM`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags("de-DE"))
    }

    @Test
    fun `SYSTEM carries no tag, so it cannot shadow a shipped language`() {
        assertEquals(null, AppLanguage.SYSTEM.languageTag)
    }
}
