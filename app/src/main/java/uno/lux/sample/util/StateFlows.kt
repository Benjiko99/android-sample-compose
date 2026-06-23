package uno.lux.sample.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
