package net.gini.android.health.api.http

import net.gini.android.core.api.GiniApiType
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Internal use only.
 *
 * OkHttp interceptor that adds Accept headers for Health API requests.
 * 
 * The Health API requires explicit Accept headers with versioned media types,
 * unlike the main Gini API which supports content negotiation.
 * 
 * This interceptor:
 * - Adds versioned Accept header if not explicitly set (e.g., "application/vnd.gini.v4+json")
 * - Preserves explicitly-set Accept headers from @Headers annotations
 */
internal class HealthApiAcceptHeaderInterceptor(
    private val giniApiType: GiniApiType
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Add versioned Accept header if not explicitly set
        // This is required for Health API endpoints
        val existingAccept = originalRequest.header("Accept")
        if (existingAccept == null) {
            requestBuilder.addHeader("Accept", giniApiType.giniJsonMediaType)
        }
        // If Accept is explicitly set (e.g., "image/jpeg" for file downloads), respect it

        return chain.proceed(requestBuilder.build())
    }
}
