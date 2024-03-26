package net.gini.android.core.api

import net.gini.android.core.api.requests.ApiException
import java.util.concurrent.CancellationException

/**
 * Represents a requested API resource. Resource requests can be returned successfully, with an error or they can be cancelled.
 */
sealed class Resource<T>(
    open val data: T? = null,
    open val responseStatusCode: Int? = null,
    open val responseHeaders: Map<String, List<String>>? = null,
    open val responseBody: String? = null
) {
    /**
     * Holds the successfully requested API resource's data and HTTP response information.
     */
    data class Success<T>(
        override val data: T,
        override val responseStatusCode: Int? = null,
        override val responseHeaders: Map<String, List<String>>? = null,
        override val responseBody: String? = null
    ) : Resource<T>(data, responseStatusCode, responseHeaders, responseBody)

    /**
     * Holds the unsuccessfully requested API resource's error details.
     */
    data class Error<T> constructor(
        val message: String? = null,
        override val responseStatusCode: Int? = null,
        override val responseHeaders: Map<String, List<String>>? = null,
        override val responseBody: String? = null,
        val exception: Exception? = null
    ) : Resource<T>(null, responseStatusCode, responseHeaders, responseBody) {

        /**
         * Internal use only.
         */
        constructor(error: Error<*>) : this(
            error.message,
            error.responseStatusCode,
            error.responseHeaders,
            error.responseBody,
            error.exception
        )

        companion object {
            /**
             * Internal use only.
             */
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

    /**
     * The API resource request was cancelled.
     */
    class Cancelled<T> : Resource<T>()

    /**
     * Utility method to chain API resource requests by mapping the successful resource request to the
     * resource returned by the block.
     *
     * Errors or cancellations are forwarded via copying and will prevent subsequent success mappings.
     * For example if requests for `Resource<A>`, `Resource<B>`, and `Resource<C>` are chained, then if `Resource<A>`
     * fails the success mapping blocks for `Resource<B>` and `Resource<C>` won't be called. Instead the error will be
     * copied to `Resource<B>`'s and then to `Resource<C>`'s [Error] instance.
     * The returned [Resource] will be a `Resource<C>.Error` instance.
     */
    suspend inline fun <U> mapSuccess(crossinline block: suspend (Success<T>) -> Resource<U>): Resource<U> {
        return when (this) {
            is Cancelled -> Cancelled()
            is Error -> Error(this)
            is Success -> block(this)
        }
    }

    companion object {
        /**
         * Internal use only.
         */
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
