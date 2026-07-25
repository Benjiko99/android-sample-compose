package uno.lux.sample.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uno.lux.sample.R
import uno.lux.sample.app.util.AppError
import uno.lux.sample.app.util.CompactCount
import uno.lux.sample.app.util.RelativeTime

/** Resolves a [RelativeTime] bucket to localized compact text ("now", "5m", …). */
@Composable
fun RelativeTime.asText(): String = when (this) {
    RelativeTime.Now -> stringResource(R.string.time_now)
    is RelativeTime.Minutes -> stringResource(R.string.time_minutes, value)
    is RelativeTime.Hours -> stringResource(R.string.time_hours, value)
    is RelativeTime.Days -> stringResource(R.string.time_days, value)
    is RelativeTime.Weeks -> stringResource(R.string.time_weeks, value)
}

/** Resolves a [CompactCount] to localized text, attaching the unit suffix from resources. */
@Composable
fun CompactCount.asText(): String = when (this) {
    is CompactCount.Ones -> text
    is CompactCount.Thousands -> stringResource(R.string.count_thousands, text)
    is CompactCount.Millions -> stringResource(R.string.count_millions, text)
}

/** Maps an [AppError] to its localized user-facing message. */
@Composable
fun AppError.asText(): String = stringResource(
    when (this) {
        AppError.NoConnection -> R.string.error_no_connection
        AppError.Timeout -> R.string.error_timeout
        AppError.Unknown -> R.string.error_unknown
    },
)

/**
 * A media duration as colon-separated digits: 95 -> "1:35", 615 -> "10:15", 3723 -> "1:02:03".
 * Minutes are zero-padded only past the hour mark; seconds always are. The colon form is
 * locale-neutral, so unlike the bucketed formatters above this returns display text directly.
 * Negative inputs are coerced to zero.
 */
fun formatVideoDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3_600
    val minutes = safe % 3_600 / 60
    val seconds = safe % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/**
 * Up to two uppercase initials drawn from the first words of a display name:
 * "Ada Lovelace" -> "AL", "Linus" -> "L", "  grace  hopper " -> "GH". Blank input -> "".
 */
fun initials(name: String): String =
    name
        .trim()
        .split(' ')
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString(separator = "")
