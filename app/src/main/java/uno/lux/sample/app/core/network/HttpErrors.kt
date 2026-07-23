package uno.lux.sample.app.core.network

import retrofit2.HttpException
import java.net.HttpURLConnection.HTTP_NOT_FOUND

/**
 * Runs [request], turning the server's 404 into `null`. "There is no such resource" is an answer,
 * not a failure: a caller has to tell it apart from a request that never got through, because only
 * one of the two is worth a retry button. Every other status still throws.
 */
internal suspend fun <T> notFoundAsNull(request: suspend () -> T): T? =
    try {
        request()
    } catch (e: HttpException) {
        if (e.code() == HTTP_NOT_FOUND) null else throw e
    }
