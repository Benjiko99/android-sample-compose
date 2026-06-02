package uno.lux.sample.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Grace period the upstream stays active after the last subscriber leaves (e.g. a config change). */
private const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

/**
 * Shares this [Flow] as a [StateFlow] in [scope], started
 * [WhileSubscribed][SharingStarted.WhileSubscribed] with a brief grace period — the standard
 * policy for exposing UI state from a ViewModel.
 */
fun <T> Flow<T>.stateInWhileSubscribed(scope: CoroutineScope, initialValue: T): StateFlow<T> =
    stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS), initialValue)
