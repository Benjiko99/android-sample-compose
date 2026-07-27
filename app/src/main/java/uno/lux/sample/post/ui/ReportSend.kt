package uno.lux.sample.post.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import uno.lux.sample.app.util.ignoreErrors
import uno.lux.sample.app.util.launchIfIdle
import kotlin.reflect.KMutableProperty0

/**
 * How far the report the user filled in has got. The dialog owns the reason and the details; this
 * is what says whether they may still be sent, and whether the dialog may close.
 *
 * The dialog stays up for the whole of [SENDING] with Send disabled, so one tap is one report,
 * and closes on [SENT] — the moment the server has actually taken it, which is what the thanks
 * claims. [FAILED] leaves the dialog exactly as the user filled it in, the failure named above
 * the buttons and Send live again, because a report already composed is one tap from being sent
 * a second time.
 *
 * The failure is a state of its own rather than a return to [IDLE], which is where this differs
 * from [CommentSendState]: the dialog is a window over the screen, so the snackbar that announces
 * a failed comment would announce a failed report behind the scrim, where the user reading the
 * dialog cannot see it.
 */
enum class ReportSendState {
    IDLE,
    SENDING,
    SENT,
    FAILED,
}

/**
 * Sends the report [block] describes, moving [state] through the send and keeping the [Job] in
 * [jobRef] for [dropReport] to abandon. A screen has one report in flight at a time — the dialog
 * is modal, so there is only ever one report being filled in.
 *
 * Not `launchReporting`: the dialog the tap came from is still on screen when the answer arrives,
 * so the failure has somewhere of its own to show and needs no snackbar. [launchIfIdle] is the
 * second guard behind the disabled Send button, so a report cannot be sent twice over.
 */
fun ViewModel.launchReport(
    jobRef: KMutableProperty0<Job?>,
    state: MutableStateFlow<ReportSendState>,
    block: suspend () -> Unit,
) = launchIfIdle(jobRef) {
    state.value = ReportSendState.SENDING

    ignoreErrors(onError = { state.value = ReportSendState.FAILED }) {
        block()
        state.value = ReportSendState.SENT
    }
}

/**
 * Abandons whatever the dialog was showing, because the dialog is gone — cancelled, dismissed, or
 * closed on the thanks. Cancelling matters for the dismissal that lands mid-send: without it the
 * answer to a report nobody is waiting for any more would still write [state], and the *next*
 * dialog would open on the outcome of the last one. [ignoreErrors] rethrows cancellation rather
 * than recording it, so the reset below is the only thing left to write [state].
 */
fun dropReport(jobRef: KMutableProperty0<Job?>, state: MutableStateFlow<ReportSendState>) {
    jobRef.get()?.cancel()
    jobRef.set(null)
    state.value = ReportSendState.IDLE
}
