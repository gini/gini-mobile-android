package net.gini.android.core.api.http

import net.gini.android.core.api.GiniApiType
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Internal use only.
 *
 * OkHttp network interceptor that ensures API-specific headers are present on all requests.
 * Uses [GiniApiType] to determine the correct versioned media types.
 *
 * Registered as a network interceptor to run after Retrofit adds its default headers,
 * ensuring we can properly replace Retrofit's "application/json" with versioned media types.
 *
 * This interceptor:
 * - Adds Accept header with versioned media type if not present (e.g., "application/vnd.gini.v4+json")
 * - Replaces Retrofit's "application/json" Content-Type with versioned media type
 * - Preserves explicitly-set non-JSON Content-Type headers (e.g., for file uploads)
 * - Supports dynamic API versions through GiniApiType
 */
internal class GiniApiHeaderInterceptor(
    private val giniApiType: GiniApiType
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Add Accept header if not already present
        if (originalRequest.header("Accept") == null) {
            requestBuilder.header("Accept", giniApiType.giniJsonMediaType)
        }

        // Add or replace Content-Type header
        // Retrofit's Moshi converter sets "application/json" but Gini API requires versioned media type
        val existingContentType = originalRequest.header("Content-Type")
        if (existingContentType == null || existingContentType.startsWith("application/json")) {
            requestBuilder.removeHeader("Content-Type")
            requestBuilder.addHeader("Content-Type", giniApiType.giniJsonMediaType)
        }
        // If Content-Type is explicitly set to something else (not application/json), respect it

        return chain.proceed(requestBuilder.build())
    }
}
