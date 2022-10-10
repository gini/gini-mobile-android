package net.gini.android.core.api.requests

import retrofit2.Response

object SafeApiRequest {
    @Throws(ApiException::class, Exception::class)
    inline fun <T : Any?> apiRequest(call: () -> Response<T>): Response<T> = try {
        val response = call.invoke()

        if (!response.isSuccessful) {
            throw ApiException(response = response)
        }

        response
    } catch (e: Exception) {
        throw e
    }
}
