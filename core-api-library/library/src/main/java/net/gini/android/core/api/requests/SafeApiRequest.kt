package net.gini.android.core.api.requests

import retrofit2.Response

/**
 * Internal use only.
 */
object SafeApiRequest {
    @Throws(ApiException::class, Exception::class)
    suspend inline fun <T : Any?> apiRequest(crossinline call: suspend () -> Response<T>): Response<T> = try {
        val response = call()

        if (!response.isSuccessful) {
            throw ApiException.forResponse(response = response)
        }

        response
    } catch (e: Exception) {
        throw e
    }
}
