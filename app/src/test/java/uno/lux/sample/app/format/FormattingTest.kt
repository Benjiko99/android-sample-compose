package uno.lux.sample.app.format

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    @Test
    fun `initials take the first letter of the first two words`() {
        assertEquals("AL", initials("Ada Lovelace"))
    }

    @Test
    fun `initials of a single word is one letter`() {
        assertEquals("L", initials("Linus"))
    }

    @Test
    fun `initials uppercase and ignore surrounding and repeated whitespace`() {
        assertEquals("GH", initials("  grace   hopper "))
    }

    @Test
    fun `initials of a blank name is empty`() {
        assertEquals("", initials("   "))
    }

    @Test
    fun `duration under a minute zero-pads the seconds`() {
        assertEquals("0:05", formatVideoDuration(5))
    }

    @Test
    fun `duration shows minutes and zero-padded seconds`() {
        assertEquals("1:35", formatVideoDuration(95))
    }

    @Test
    fun `duration keeps minutes un-padded below the hour`() {
        assertEquals("10:15", formatVideoDuration(615))
    }

    @Test
    fun `duration past an hour pads minutes and seconds`() {
        assertEquals("1:02:03", formatVideoDuration(3_723))
    }

    @Test
    fun `negative duration is coerced to zero`() {
        assertEquals("0:00", formatVideoDuration(-5))
    }
}
