package uno.lux.sample.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uno.lux.sample.R

/**
 * A reusable confirmation dialog shown when a user attempts to navigate away from a screen
 * with unsaved changes.
 */
@Composable
fun DiscardChangesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.discard_confirmation_title)) },
        text = { Text(stringResource(R.string.discard_confirmation_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.discard_confirmation_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.discard_confirmation_dismiss))
            }
        },
    )
}
