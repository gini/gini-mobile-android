package net.gini.android.core.api

import net.gini.android.core.api.requests.ApiException
import java.util.concurrent.CancellationException

sealed class Resource<T>(
    open val data: T? = null,
    open val responseStatusCode: Int? = null,
    open val responseHeaders: Map<String, List<String>>? = null,
    open val responseBody: String? = null
) {
    data class Success<T>(
        override val data: T,
        override val responseStatusCode: Int? = null,
        override val responseHeaders: Map<String, List<String>>? = null,
        override val responseBody: String? = null
    ) : Resource<T>(data, responseStatusCode, responseHeaders, responseBody)

    data class Error<T> constructor(
        val message: String? = null,
        override val responseStatusCode: Int? = null,
        override val responseHeaders: Map<String, List<String>>? = null,
        override val responseBody: String? = null,
        val exception: Exception? = null
    ) : Resource<T>(null, responseStatusCode, responseHeaders, responseBody) {
        constructor(error: Error<*>) : this(
            error.message,
            error.responseStatusCode,
            error.responseHeaders,
            error.responseBody,
            error.exception
        )

        companion object {
            fun <T> fromApiException(apiException: ApiException): Error<T> =
                Error(
                    apiException.message,
                    apiException.responseStatusCode,
                    apiException.responseHeaders,
                    apiException.responseBody,
                    apiException
                )
        }
    }

    class Cancelled<T> : Resource<T>()

    companion object {
        suspend inline fun <T> wrapInResource(crossinline request: suspend () -> T) =
            try {
                Success(request())
            } catch (e: ApiException) {
                Error.fromApiException(e)
            } catch (e: CancellationException) {
                Cancelled()
            } catch (e: Exception) {
                Error(e.message, exception = e)
            }
    }
}
