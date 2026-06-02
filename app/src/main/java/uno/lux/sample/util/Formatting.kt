package uno.lux.sample.util

import java.time.Duration
import java.time.Instant

/**
 * A relative timestamp bucketed for compact, social-style display. The bucketing is pure
 * and unit-tested here; turning a bucket into localized text ("now", "5m", …) happens in
 * the UI layer against string resources.
 */
sealed interface RelativeTime {
    data object Now : RelativeTime
    data class Minutes(val value: Long) : RelativeTime
    data class Hours(val value: Long) : RelativeTime
    data class Days(val value: Long) : RelativeTime
    data class Weeks(val value: Long) : RelativeTime
}

/** Buckets the gap between [createdAt] and [now]; [now] is injectable for deterministic tests. */
fun relativeTime(createdAt: Instant, now: Instant = Instant.now()): RelativeTime {
    val elapsed = Duration.between(createdAt, now)
    return when {
        elapsed < Duration.ofMinutes(1) -> RelativeTime.Now
        elapsed < Duration.ofHours(1) -> RelativeTime.Minutes(elapsed.toMinutes())
        elapsed < Duration.ofDays(1) -> RelativeTime.Hours(elapsed.toHours())
        elapsed < Duration.ofDays(7) -> RelativeTime.Days(elapsed.toDays())
        else -> RelativeTime.Weeks(elapsed.toDays() / 7)
    }
}

/**
 * An engagement count split into its numeric [text] and scale, so the unit suffix (K/M)
 * comes from localized resources instead of being baked into code. Negative inputs are
 * coerced to zero.
 */
sealed interface CompactCount {
    val text: String
    data class Ones(override val text: String) : CompactCount
    data class Thousands(override val text: String) : CompactCount
    data class Millions(override val text: String) : CompactCount
}

fun compactCount(count: Int): CompactCount = when {
    count < 1_000 -> CompactCount.Ones(count.coerceAtLeast(0).toString())
    count < 1_000_000 -> CompactCount.Thousands(scaled(count, unit = 1_000))
    else -> CompactCount.Millions(scaled(count, unit = 1_000_000))
}

/** The count scaled to [unit] with at most one decimal place: 1_234 / 1_000 -> "1.2". */
private fun scaled(count: Int, unit: Int): String {
    val whole = count / unit
    val tenths = count % unit / (unit / 10)
    return if (tenths == 0) "$whole" else "$whole.$tenths"
}

/**
 * Up to two uppercase initials drawn from the first words of a display name:
 * "Ada Lovelace" -> "AL", "Linus" -> "L", "  grace  hopper " -> "GH". Blank input -> "".
 */
fun initials(name: String): String =
    name.trim().split(' ')
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString(separator = "")
