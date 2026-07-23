package uno.lux.sample.design.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uno.lux.sample.R
import uno.lux.sample.post.ReportReason
import uno.lux.sample.composer.ui.CreatePostError
import uno.lux.sample.composer.ui.CreatePostMaxVideoBytes
import uno.lux.sample.util.AppError
import uno.lux.sample.util.CompactCount
import uno.lux.sample.util.RelativeTime

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
    }
)

/** Maps a [CreatePostError] to its localized message, filling in any limit it names. */
@Composable
fun CreatePostError.asText(): String = when (this) {
    is CreatePostError.Failed -> error.asText()
    CreatePostError.VideoTooLarge -> stringResource(
        R.string.create_post_video_too_large,
        CreatePostMaxVideoBytes / (1024 * 1024),
    )
}

/** Maps a [ReportReason] to its localized display label. */
@Composable
fun ReportReason.asText(): String = stringResource(
    when (this) {
        ReportReason.SPAM -> R.string.report_reason_spam
        ReportReason.HARASSMENT -> R.string.report_reason_harassment
        ReportReason.HATE_SPEECH -> R.string.report_reason_hate_speech
        ReportReason.MISINFORMATION -> R.string.report_reason_misinformation
        ReportReason.VIOLENCE -> R.string.report_reason_violence
        ReportReason.OTHER -> R.string.report_reason_other
    }
)
