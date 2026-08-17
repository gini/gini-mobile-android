package net.gini.android.core.api.requests

import retrofit2.Response
import java.io.IOException

/**
 * Api request exception. Thrown when completing an api request fails.
 */
class ApiException(
    message: String? = null,
    val responseStatusCode: Int? = null,
    val responseBody: String? = null,
    val responseHeaders: Map<String, List<String>>? = null,
    cause: Throwable? = null
) : IOException(message, cause) {

    companion object {
        /**
         * Internal use only.
         */
        fun forResponse(message: String? = null, response: Response<*>): ApiException {
            val body = if (response.isSuccessful) response.body()?.toString() else response.errorBody()?.string()
            return ApiException(message ?: body, response.code(), body, response.headers().toMultimap())
        }
    }
}

/**
 * Internal use only.
 *
 * Thrown when the session request was cancelled while authenticating an API request in the
 * OkHttp layer. It extends [IOException] because OkHttp interceptors may only throw
 * [IOException]s ([kotlinx.coroutines.CancellationException] would be wrapped by OkHttp and
 * lose its meaning). [net.gini.android.core.api.Resource.Companion.wrapInResource] maps it to
 * [net.gini.android.core.api.Resource.Cancelled].
 */
class SessionCancellationException : IOException("Session request was cancelled")
