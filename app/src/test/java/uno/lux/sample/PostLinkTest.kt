package uno.lux.sample

import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.util.postLink

class PostLinkTest {

    @Test
    fun `post link is the placeholder host plus the post path`() {
        assertEquals("https://mosaic.app/p/p1", postLink("p1"))
    }

    @Test
    fun `post link appends the id verbatim`() {
        assertEquals("https://mosaic.app/p/abc-123", postLink("abc-123"))
    }
}
