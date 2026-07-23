package uno.lux.sample.app.util

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Typed error result for any operation that can fail. Used by ViewModels to carry structured
 * failure information instead of a bare boolean, so the UI layer can map each case to a
 * localized message via [uno.lux.sample.design.format.asText].
 */
sealed interface AppError {
    data object NoConnection : AppError
    data object Timeout : AppError
    data object Unknown : AppError
}

fun Throwable.toAppError(): AppError = when (this) {
    is UnknownHostException, is ConnectException -> AppError.NoConnection
    is SocketTimeoutException -> AppError.Timeout
    else -> AppError.Unknown
}
