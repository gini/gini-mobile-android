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
 * configuration on top. Your configuration will be preserved, and the SDK will layer its
 * requirements using [OkHttpClient.newBuilder].
 *
 * The SDK adds the following configuration:
 * - **User-Agent header**: Identifies requests as coming from the Gini SDK
 * - **GiniApiHeaderInterceptor** (network interceptor): Replaces Retrofit's default
 *   `application/json` Content-Type with versioned media types (e.g., `application/vnd.gini.v4+json`)
 * - **GiniAuthenticationInterceptor** (application interceptor): Automatically adds Bearer
 *   authentication tokens to all API requests
 * - **GiniAuthenticator**: Handles automatic token refresh on 401 Unauthorized responses
 *
 * **Interceptor ordering** (important for custom interceptors):
 * 1. Your application interceptors (from provided client) - run first
 * 2. SDK's authentication interceptor - adds Bearer token
 * 3. SDK's network interceptor - replaces Content-Type header
 * 4. SDK's authenticator - handles 401 token refresh
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
 * - Your application interceptors will run BEFORE SDK interceptors (giving you visibility into all requests)
 * - Your network interceptors will run BEFORE SDK network interceptors
 * - The SDK will automatically set Content-Type to versioned media types (e.g., `application/vnd.gini.v4+json`)
 *   for JSON requests. Non-JSON Content-Types (e.g., `multipart/form-data`) are preserved as-is
 * - If you need logging, add [okhttp3.logging.HttpLoggingInterceptor] as an application interceptor
 *   so it can see SDK-added headers
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
