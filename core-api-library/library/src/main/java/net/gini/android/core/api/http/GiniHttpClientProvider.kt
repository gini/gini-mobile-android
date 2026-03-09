package net.gini.android.core.api.http

import okhttp3.OkHttpClient

/**
 * Provider interface for supplying a custom [OkHttpClient] to the Gini API libraries.
 *
 * This allows consumers to have full control over HTTP client configuration including:
 * - TLS/SSL configuration and certificate pinning
 * - Proxy settings
 * - Custom interceptors for logging, authentication, or request modification
 * - Connection pooling and timeouts
 * - Cache configuration
 *
 * **Important**: The SDK will use your provided client as a base and add its own required
 * configuration on top (such as User-Agent header). Your configuration will be preserved,
 * and the SDK will layer its requirements using [OkHttpClient.newBuilder].
 *
 * ## Usage Example
 *
 * ```kotlin
 * class CustomHttpClientProvider : GiniHttpClientProvider {
 *     override fun provideOkHttpClient(): OkHttpClient {
 *         return OkHttpClient.Builder()
 *             .addInterceptor(myCustomLoggingInterceptor)  // Your interceptor
 *             .proxy(myProxy)                              // Your proxy
 *             .sslSocketFactory(mySslSocketFactory, myTrustManager)  // Your TLS config
 *             .connectTimeout(30, TimeUnit.SECONDS)        // Your timeouts
 *             .build()
 *         // SDK will add User-Agent and other required config on top
 *     }
 * }
 *
 * // Then inject it into the builder:
 * val giniBankApi = GiniBankAPIBuilder(context, clientId, clientSecret, emailDomain)
 *     .setHttpClientProvider(CustomHttpClientProvider())
 *     .build()
 * ```
 *
 * ## Important Notes
 *
 * - The provided [OkHttpClient] should be properly configured with reasonable timeouts
 * - The SDK will add required headers (User-Agent) only if you haven't already set them
 * - Your interceptors will run before the SDK's required interceptors
 * - If you need logging, consider using [okhttp3.logging.HttpLoggingInterceptor]
 * - The client may be shared across multiple Gini API instances if desired
 *
 * @see DefaultGiniHttpClientProvider for the SDK's default implementation
 */
interface GiniHttpClientProvider {
    /**
     * Provides a configured [OkHttpClient] instance for use by the Gini API libraries.
     *
     * The SDK will use this client as a base and add its own required configuration
     * on top using [OkHttpClient.newBuilder]. This ensures both your custom
     * configuration and the SDK's requirements are satisfied.
     *
     * This method may be called multiple times, so implementations should either:
     * - Return the same cached instance (recommended for most use cases)
     * - Create a new instance each time (only if you have specific requirements)
     *
     * @return A configured [OkHttpClient] instance that will serve as the base
     *         for the SDK's HTTP client
     */
    fun provideOkHttpClient(): OkHttpClient
}
