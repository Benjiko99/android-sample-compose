package uno.lux.sample

import org.junit.Assert.assertEquals
import org.junit.Test
import uno.lux.sample.util.CompactCount
import uno.lux.sample.util.RelativeTime
import uno.lux.sample.util.compactCount
import uno.lux.sample.util.initials
import uno.lux.sample.util.relativeTime
import java.time.Duration
import java.time.Instant

class FormattingTest {

    private val now: Instant = Instant.parse("2026-06-02T12:00:00Z")

    @Test
    fun `relative time under a minute is Now`() {
        assertEquals(RelativeTime.Now, relativeTime(now.minus(Duration.ofSeconds(30)), now))
    }

    @Test
    fun `relative time rounds down to whole minutes`() {
        assertEquals(
            RelativeTime.Minutes(5),
            relativeTime(now.minus(Duration.ofSeconds(5 * 60 + 59)), now),
        )
    }

    @Test
    fun `relative time in hours`() {
        assertEquals(RelativeTime.Hours(3), relativeTime(now.minus(Duration.ofHours(3)), now))
    }

    @Test
    fun `relative time in days`() {
        assertEquals(RelativeTime.Days(2), relativeTime(now.minus(Duration.ofDays(2)), now))
    }

    @Test
    fun `relative time switches to weeks at seven days`() {
        assertEquals(RelativeTime.Weeks(2), relativeTime(now.minus(Duration.ofDays(14)), now))
    }

    @Test
    fun `counts below a thousand are exact`() {
        assertEquals(CompactCount.Ones("999"), compactCount(999))
    }

    @Test
    fun `thousands keep one decimal place`() {
        assertEquals(CompactCount.Thousands("1.2"), compactCount(1_200))
    }

    @Test
    fun `whole thousands drop the decimal`() {
        assertEquals(CompactCount.Thousands("12"), compactCount(12_000))
    }

    @Test
    fun `millions are scaled`() {
        assertEquals(CompactCount.Millions("1"), compactCount(1_000_000))
    }

    @Test
    fun `negative counts are coerced to zero`() {
        assertEquals(CompactCount.Ones("0"), compactCount(-5))
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
