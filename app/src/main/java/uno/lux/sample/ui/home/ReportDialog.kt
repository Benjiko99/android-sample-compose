package uno.lux.sample.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uno.lux.sample.R
import uno.lux.sample.data.ReportReason
import uno.lux.sample.ui.theme.MosaicTheme
import uno.lux.sample.util.ActionsInvocationHandler

/**
 * The outcomes a [ReportPostDialog] reports back, bundled into one [Stable] interface so the
 * dialog takes a single parameter and a preview can hand it a no-op proxy from
 * [ActionsInvocationHandler.createActionsProxy].
 */
@Stable
interface ReportActions {
    fun dismiss()
    fun submit(reason: ReportReason, details: String)
}

/**
 * The report-a-post dialog: a single-choice list of [ReportReason]s plus an optional free-text
 * field. The chosen reason and trimmed details are handed to [ReportActions.submit]; Send stays
 * disabled until a reason is picked. The transient selection is owned here, so the host deals
 * only with the submit / dismiss outcomes.
 */
@Composable
internal fun ReportPostDialog(
    actions: ReportActions,
    modifier: Modifier = Modifier,
) {
    var selectedReason by remember { mutableStateOf<ReportReason?>(null) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = actions::dismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.report_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.report_dialog_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                ReportReason.entries.forEach { reason ->
                    ReasonRow(
                        label = stringResource(reason.labelRes()),
                        selected = reason == selectedReason,
                        onSelect = { selectedReason = reason },
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text(stringResource(R.string.report_details_hint)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedReason?.let { actions.submit(it, details.trim()) } },
                enabled = selectedReason != null,
            ) {
                Text(stringResource(R.string.report_send))
            }
        },
        dismissButton = {
            TextButton(onClick = actions::dismiss) {
                Text(stringResource(R.string.report_cancel))
            }
        },
    )
}

/** One selectable reason; the whole row is the radio target so the tap area isn't just the dot. */
@Composable
private fun ReasonRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** The localized label for each [ReportReason] — kept in the UI so the domain enum stays Android-free. */
@StringRes
private fun ReportReason.labelRes(): Int = when (this) {
    ReportReason.SPAM -> R.string.report_reason_spam
    ReportReason.HARASSMENT -> R.string.report_reason_harassment
    ReportReason.HATE_SPEECH -> R.string.report_reason_hate_speech
    ReportReason.MISINFORMATION -> R.string.report_reason_misinformation
    ReportReason.VIOLENCE -> R.string.report_reason_violence
    ReportReason.OTHER -> R.string.report_reason_other
}

@Preview(showBackground = true)
@Composable
private fun ReportPostDialogPreview() {
    MosaicTheme {
        ReportPostDialog(actions = ActionsInvocationHandler.createActionsProxy())
    }
}
