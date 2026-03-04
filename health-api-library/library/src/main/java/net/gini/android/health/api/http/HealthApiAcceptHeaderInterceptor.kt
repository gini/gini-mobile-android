package net.gini.android.health.api.http

import net.gini.android.core.api.GiniApiType
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Internal use only.
 *
 * OkHttp interceptor that adds Accept headers for the Health API.
 * 
 * The Health API requires explicit Accept headers on requests, unlike the main Gini API
 * which supports content negotiation. This interceptor ensures all Health API requests
 * include the correct versioned Accept header.
 */
internal class HealthApiAcceptHeaderInterceptor(
    private val giniApiType: GiniApiType
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Add Accept header if not already present
        val request = if (originalRequest.header("Accept") == null) {
            originalRequest.newBuilder()
                .header("Accept", giniApiType.giniJsonMediaType)
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(request)
    }
}
