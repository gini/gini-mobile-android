package net.gini.android.core.api.http

import net.gini.android.core.api.GiniApiType
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Internal use only.
 *
 * OkHttp interceptor that automatically adds API-specific headers to all requests.
 * Uses [GiniApiType] to determine the correct versioned media types.
 *
 * This interceptor:
 * - Adds Accept header with versioned media type (e.g., "application/vnd.gini.v4+json")
 * - Adds Content-Type header with versioned media type for non-GET requests
 * - Only adds headers if not already present (allows manual override)
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

        // Add Content-Type header for non-GET requests if not already present
        // GET requests typically don't need Content-Type, but some Gini APIs expect it
        if (originalRequest.header("Content-Type") == null) {
            requestBuilder.header("Content-Type", giniApiType.giniJsonMediaType)
        }

        return chain.proceed(requestBuilder.build())
    }
}
