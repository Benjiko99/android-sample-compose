package uno.lux.sample.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.reflect.KMutableProperty0

/** Grace period the upstream stays active after the last subscriber leaves (e.g. a config change). */
private const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

/**
 * Shares this [Flow] as a [StateFlow] in [scope], started
 * [WhileSubscribed][SharingStarted.WhileSubscribed] with a brief grace period — the standard
 * policy for exposing UI state from a ViewModel.
 */
fun <T> Flow<T>.stateInWhileSubscribed(scope: CoroutineScope, initialValue: T): StateFlow<T> =
    stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS), initialValue)

/**
 * Launches [block] in [viewModelScope], setting [refreshing] to `true` for its duration.
 * Keeps the pull-to-refresh bookkeeping out of ViewModel bodies.
 */
fun ViewModel.launchRefresh(refreshing: MutableStateFlow<Boolean>, block: suspend () -> Unit) {
    viewModelScope.launch {
        refreshing.value = true
        try { block() } finally { refreshing.value = false }
    }
}

/**
 * Launches [block] in [viewModelScope], writing the resulting [Job] into [jobRef], but only if
 * the previous job stored there is no longer active. Subsequent calls while the job is running
 * are silently ignored, preventing duplicate in-flight requests (e.g. repeated load-more
 * triggers before the first page completes).
 */
fun ViewModel.launchIfIdle(jobRef: KMutableProperty0<Job?>, block: suspend () -> Unit) {
    if (jobRef.get()?.isActive == true) return
    jobRef.set(viewModelScope.launch { block() })
}

/**
 * Runs [block], discarding any non-[CancellationException] thrown. Calls [onError] before
 * discarding so callers can update error state without repeating the rethrow boilerplate.
 */
suspend fun ignoreErrors(onError: (Exception) -> Unit = {}, block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onError(e)
    }
}

/**
 * Runs [block], writing any non-[CancellationException] into [errorSink] as an [AppError].
 * Convenience overload for the common ViewModel pattern of storing a typed error alongside
 * a reactive combine chain.
 */
suspend fun ignoreErrors(errorSink: MutableStateFlow<AppError?>, block: suspend () -> Unit) =
    ignoreErrors(
        onError = { e ->
            Timber.w(e)
            errorSink.value = e.toAppError()
        },
        block = block,
    )
