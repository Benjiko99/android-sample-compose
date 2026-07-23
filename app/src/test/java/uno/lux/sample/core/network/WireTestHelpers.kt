package uno.lux.sample.core.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import uno.lux.sample.app.core.network.CursorPageDto

internal val emptyPage = CursorPageDto(nextCursor = null, hasMore = false)

/**
 * The 404 Retrofit raises for an unknown id, error envelope and all — what a data source has to
 * turn back into a "there is no such thing" answer.
 */
fun notFoundException(message: String) = HttpException(
    Response.error<Any>(
        404,
        """{"error":{"code":"NOT_FOUND","message":"$message"}}"""
            .toResponseBody("application/json".toMediaType()),
    )
)
