package uno.lux.sample.post.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uno.lux.sample.R
import uno.lux.sample.app.common.ReportReason

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
