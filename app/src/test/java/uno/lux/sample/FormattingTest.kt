package uno.lux.sample

import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.util.formatCount
import uno.lux.sample.util.formatRelativeTime
import uno.lux.sample.util.initials
import java.time.Duration
import java.time.Instant

class FormattingTest {

    private val now: Instant = Instant.parse("2026-06-02T12:00:00Z")

    @Test
    fun `relative time under a minute reads as now`() {
        assertEquals("now", formatRelativeTime(now.minus(Duration.ofSeconds(30)), now))
    }

    @Test
    fun `relative time rounds down to whole minutes`() {
        assertEquals("5m", formatRelativeTime(now.minus(Duration.ofSeconds(5 * 60 + 59)), now))
    }

    @Test
    fun `relative time in hours`() {
        assertEquals("3h", formatRelativeTime(now.minus(Duration.ofHours(3)), now))
    }

    @Test
    fun `relative time in days`() {
        assertEquals("2d", formatRelativeTime(now.minus(Duration.ofDays(2)), now))
    }

    @Test
    fun `relative time switches to weeks at seven days`() {
        assertEquals("2w", formatRelativeTime(now.minus(Duration.ofDays(14)), now))
    }

    @Test
    fun `counts below a thousand are left as-is`() {
        assertEquals("999", formatCount(999))
    }

    @Test
    fun `thousands keep one decimal place`() {
        assertEquals("1.2K", formatCount(1_200))
    }

    @Test
    fun `whole thousands drop the decimal`() {
        assertEquals("12K", formatCount(12_000))
    }

    @Test
    fun `millions are abbreviated`() {
        assertEquals("1M", formatCount(1_000_000))
    }

    @Test
    fun `negative counts are coerced to zero`() {
        assertEquals("0", formatCount(-5))
    }

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
}
