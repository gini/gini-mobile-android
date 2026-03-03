package net.gini.android.core.api.http

import net.gini.android.core.api.GiniApiType
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Internal use only.
 *
 * OkHttp network interceptor that ensures correct Content-Type headers for API requests.
 * Uses [GiniApiType] to determine the correct versioned media types.
 *
 * Registered as a network interceptor to run after Retrofit adds its default headers,
 * ensuring we can properly replace Retrofit's "application/json" with versioned media types.
 *
 * This interceptor:
 * - Replaces Retrofit's "application/json" Content-Type with versioned media type (e.g., "application/vnd.gini.v4+json")
 * - Preserves explicitly-set non-JSON Content-Type headers (e.g., for file uploads)
 * - Supports dynamic API versions through GiniApiType
 *
 * Note: This interceptor does NOT manage Accept headers for the main Gini API (pay-api.gini.net).
 * The main API works without explicit Accept headers, relying on content negotiation.
 * However, other APIs like the User Center API may still require explicit Accept headers,
 * which should be passed via @HeaderMap or set via @Headers annotation as needed.
 */
internal class GiniApiHeaderInterceptor(
    private val giniApiType: GiniApiType
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Replace Retrofit's default "application/json" Content-Type with versioned media type
        // This is required for POST/PUT/PATCH requests with JSON bodies
        val existingContentType = originalRequest.header("Content-Type")
        if (existingContentType == null || existingContentType.startsWith("application/json")) {
            requestBuilder.removeHeader("Content-Type")
            requestBuilder.addHeader("Content-Type", giniApiType.giniJsonMediaType)
        }
        // If Content-Type is explicitly set to something else (not application/json), respect it

        return chain.proceed(requestBuilder.build())
    }
}
