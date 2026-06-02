package uno.lux.sample.util

import java.time.Duration
import java.time.Instant

/**
 * Compact, social-style relative timestamp: "now", "5m", "3h", "2d", "4w".
 *
 * [now] is injectable so the result is deterministic in tests and previews.
 */
fun formatRelativeTime(createdAt: Instant, now: Instant = Instant.now()): String {
    val elapsed = Duration.between(createdAt, now)
    return when {
        elapsed < Duration.ofMinutes(1) -> "now"
        elapsed < Duration.ofHours(1) -> "${elapsed.toMinutes()}m"
        elapsed < Duration.ofDays(1) -> "${elapsed.toHours()}h"
        elapsed < Duration.ofDays(7) -> "${elapsed.toDays()}d"
        else -> "${elapsed.toDays() / 7}w"
    }
}

/**
 * Abbreviates engagement counts the way feeds do: 999 -> "999", 1_200 -> "1.2K",
 * 12_000 -> "12K", 1_000_000 -> "1M". Negative inputs are coerced to zero.
 */
fun formatCount(count: Int): String = when {
    count < 1_000 -> count.coerceAtLeast(0).toString()
    count < 1_000_000 -> abbreviate(count, unit = 1_000, suffix = "K")
    else -> abbreviate(count, unit = 1_000_000, suffix = "M")
}

private fun abbreviate(count: Int, unit: Int, suffix: String): String {
    val whole = count / unit
    val tenths = count % unit / (unit / 10)
    return if (tenths == 0) "$whole$suffix" else "$whole.$tenths$suffix"
}
