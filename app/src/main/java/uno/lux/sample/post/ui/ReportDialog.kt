package uno.lux.sample.post.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import uno.lux.sample.post.ReportReason
import uno.lux.sample.design.format.asText
import uno.lux.sample.design.theme.MosaicTheme

/**
 * The report-a-post dialog: a single-choice list of [ReportReason]s plus an optional free-text
 * field. The chosen reason and trimmed details are handed to [onSubmit]; Send stays disabled
 * until a reason is picked. The transient selection is owned here, so the host deals only with
 * the submit / dismiss outcomes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportPostDialog(
    onDismiss: () -> Unit,
    onSubmit: (reason: ReportReason, details: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedReason by remember { mutableStateOf<ReportReason?>(null) }
    val detailsState = rememberTextFieldState()

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        DialogSurface {
            ReportPostDialogContent(
                selectedReason = selectedReason,
                detailsState = detailsState,
                onSelectReason = { selectedReason = it },
                onDismiss = onDismiss,
                onSubmit = {
                    onSubmit(selectedReason!!, detailsState.text.toString().trim())
                },
            )
        }
    }
}

@Composable
private fun ReportPostDialogContent(
    selectedReason: ReportReason?,
    detailsState: TextFieldState,
    onSelectReason: (ReportReason) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = stringResource(R.string.report_dialog_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(16.dp))
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(R.string.report_dialog_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            ReportReason.entries.forEach { reason ->
                ReasonRow(
                    label = reason.asText(),
                    selected = reason == selectedReason,
                    onSelect = { onSelectReason(reason) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                state = detailsState,
                label = { Text(stringResource(R.string.report_details_hint)) },
                lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 2),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.report_cancel))
            }
            TextButton(
                onClick = onSubmit,
                enabled = selectedReason != null,
            ) {
                Text(stringResource(R.string.report_send))
            }
        }
    }
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogSurface(content: @Composable () -> Unit) {
    Surface(
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
private fun ReportPostDialogPreview() {
    MosaicTheme {
        DialogSurface {
            ReportPostDialogContent(
                selectedReason = null,
                detailsState = rememberTextFieldState(),
                onSelectReason = {},
                onDismiss = {},
                onSubmit = {},
            )
        }
    }
}
