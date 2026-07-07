package uno.lux.sample

import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.di.NetworkModule
import uno.lux.sample.util.postLink

class PostLinkTest {

    @Test
    fun `post link is the placeholder host plus the post path`() {
        assertEquals("${NetworkModule.BASE_URL}/p/p1", postLink("p1"))
    }

    @Test
    fun `post link appends the id verbatim`() {
        assertEquals("${NetworkModule.BASE_URL}/p/abc-123", postLink("abc-123"))
    }
}
