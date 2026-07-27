package uno.lux.sample.common.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import uno.lux.sample.app.util.ignoreErrors
import uno.lux.sample.app.util.launchCatching
import uno.lux.sample.common.asText

/**
 * A fire-and-forget user action whose request failed after the UI had already moved on — the
 * confirmation dialog closed, the report sheet dismissed, the follow button never moved — so
 * there is nothing left on screen for the failure to show up in. The ViewModel that ran the
 * action names it here for the screen to announce once and then spend.
 *
 * Optimistic mutations are deliberately absent: a failed like or bookmark reverts the control
 * the user just tapped, and that revert is its own announcement. The rule is whether the failure
 * is visible where the tap happened, not which screen the action belongs to.
 */
enum class FailedAction {
    DELETE_POST,
    REPORT_POST,
    FOLLOW,
}

/**
 * Launches [block], naming [action] in [sink] if it fails. [launchCatching] with somewhere to
 * report to — for the mutations whose UI is gone by the time the answer arrives, where logging
 * and discarding would leave the tap looking exactly like success.
 *
 * Takes the sink as a parameter the way [uno.lux.sample.app.util.launchRefresh] takes its
 * `refreshing` flag, so the ViewModel keeps owning the state a test asserts against. Not in
 * `app/util` with the other launch shapes because it names [FailedAction], and `app/util`
 * imports nothing of the project's — the same reason [ignoreErrors]'s error-sink overload lives
 * here rather than there.
 */
fun ViewModel.launchReporting(
    sink: MutableStateFlow<FailedAction?>,
    action: FailedAction,
    block: suspend () -> Unit,
) {
    viewModelScope.launch {
        ignoreErrors(onError = { sink.value = action }, block = block)
    }
}

/**
 * Announces [failedAction] in [snackbarHostState] once, then calls [onShown] to spend it. The
 * spend is what stops a configuration change from repeating a failure the user has already read:
 * the composition is rebuilt from nothing, so a signal still in the state would be announced
 * again on every rotation.
 *
 * No retry action, unlike a failed load: the thing to retry is a tap that is one gesture away,
 * and the control is still on screen.
 */
@Composable
fun FailedActionEffect(
    failedAction: FailedAction?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    // Resolved out here because asText() is itself @Composable, and keyed on the message so a
    // locale change mid-snackbar re-announces in the language the user just picked.
    val message = failedAction?.asText()

    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect

        snackbarHostState.showSnackbar(message)
        onShown()
    }
}
