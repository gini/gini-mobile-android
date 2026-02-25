package net.gini.android.bank.sdk.exampleapp.core

import android.util.Log
import net.gini.android.core.api.http.GiniHttpClientProvider
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Example implementation of [GiniHttpClientProvider] demonstrating how to customize
 * the HTTP client configuration for advanced use cases.
 *
 * This example shows how to configure:
 * - Custom logging with specific log levels
 * - Proxy settings
 * - Extended timeouts
 * - Custom interceptors
 *
 * **Important**: The SDK will use your client as a base and add its own required
 * configuration on top (such as User-Agent header). Your configuration will be preserved,
 * and the SDK will only add what's missing.
 *
 * This is useful when integrators need full control over HTTP behavior without
 * having to implement the entire [net.gini.android.capture.network.GiniCaptureNetworkService].
 */
class CustomHttpClientProvider(
    private val proxy: Proxy? = null,
    private val enableDetailedLogging: Boolean = false,
    private val customTimeoutSeconds: Long = 60
) : GiniHttpClientProvider {

    private val client: OkHttpClient by lazy {
        createOkHttpClient()
    }

    override fun provideOkHttpClient(): OkHttpClient {
        return client
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .apply {
                // Custom timeout configuration
                connectTimeout(customTimeoutSeconds, TimeUnit.SECONDS)
                readTimeout(customTimeoutSeconds, TimeUnit.SECONDS)
                writeTimeout(customTimeoutSeconds, TimeUnit.SECONDS)

                // Proxy configuration (if provided)
                proxy?.let { proxy(it) }

                // Custom logging configuration
                if (enableDetailedLogging) {
                    addInterceptor(LoggingInterceptor())
                }

                // Example: Add custom header to all requests
                addInterceptor { chain ->
                    val originalRequest = chain.request()
                    val requestWithHeaders = originalRequest.newBuilder()
                        .header("X-Custom-Header", "CustomValue")
                        .build()
                    chain.proceed(requestWithHeaders)
                }

                // Additional custom configurations can be added here:
                // - TLS/SSL configuration: sslSocketFactory(...), hostnameVerifier(...)
                // - Certificate pinning: certificatePinner(...)
                // - Connection pooling: connectionPool(...)
                // - Cache: cache(...)
                // - Event listeners: eventListener(...)
            }
            .build()
    }

    /**
     * Simple logging interceptor that logs HTTP requests and responses.
     * For production use with more features, add the okhttp-logging-interceptor dependency
     * and use okhttp3.logging.HttpLoggingInterceptor instead.
     */
    private class LoggingInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            
            val t1 = System.nanoTime()
            Log.d(TAG, "Sending request ${request.url} ${request.method}")
            
            val response = chain.proceed(request)
            
            val t2 = System.nanoTime()
            Log.d(TAG, "Received response for ${response.request.url} in ${(t2 - t1) / 1e6}ms")
            Log.d(TAG, "Response code: ${response.code}")
            
            return response
        }
        
        companion object {
            private const val TAG = "CustomHttpClient"
        }
    }
}
