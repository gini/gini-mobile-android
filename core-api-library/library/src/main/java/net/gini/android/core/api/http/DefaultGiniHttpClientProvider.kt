package net.gini.android.core.api.http

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.XmlRes
import net.gini.android.core.api.authorization.PubKeyManager
import net.gini.android.core.api.authorization.X509TrustManagerAdapter
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager

/**
 * Default implementation of [GiniHttpClientProvider] that creates an [OkHttpClient] with
 * configurable settings matching the SDK's standard configuration.
 *
 * This provider encapsulates the SDK's default HTTP client configuration including:
 * - User-Agent header injection
 * - TLS/SSL configuration with optional certificate pinning
 * - Connection timeouts
 * - Optional caching
 * - Optional debug logging
 *
 * Use the [Builder] to configure and create instances.
 *
 * ## Example
 *
 * ```kotlin
 * val provider = DefaultGiniHttpClientProvider.builder(context)
 *     .setConnectionTimeoutInMs(30000)
 *     .setCache(cache)
 *     .setDebuggingEnabled(BuildConfig.DEBUG)
 *     .build()
 * ```
 *
 * @param context Android context for accessing system resources
 * @param hostnames List of hostnames for certificate pinning (optional)
 * @param networkSecurityConfigResId Resource ID for network security config (optional)
 * @param cache OkHttp cache instance (optional)
 * @param trustManager Custom trust manager (optional)
 * @param connectionTimeoutInMs Connection timeout in milliseconds
 * @param isDebuggingEnabled Whether to enable HTTP request/response logging
 */
class DefaultGiniHttpClientProvider private constructor(
    private val context: Context,
    private val hostnames: List<String>,
    @XmlRes private val networkSecurityConfigResId: Int,
    private val cache: Cache?,
    private val trustManager: TrustManager?,
    private val connectionTimeoutInMs: Int,
    private val isDebuggingEnabled: Boolean
) : GiniHttpClientProvider {

    private val httpLoggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val cachedClient: OkHttpClient by lazy {
        createOkHttpClient()
    }

    override fun provideOkHttpClient(): OkHttpClient {
        return cachedClient
    }

    private fun createOkHttpClient() = OkHttpClient.Builder()
        .apply {
            // Set system user agent string or fallback user agent if it's not available
            addInterceptor { chain ->
                chain.proceed(
                    chain.request()
                        .newBuilder()
                        .header(
                            "User-Agent",
                            System.getProperty("http.agent") ?: FALLBACK_USER_AGENT
                        )
                        .build()
                )
            }

            getTrustManagers()?.let { trustManagers ->
                createSSLSocketFactory(trustManagers)?.let { socketFactory ->
                    sslSocketFactory(socketFactory, X509TrustManagerAdapter(trustManagers[0]))
                }
            }

            cache?.let { cache(it) }

            if (isDebuggingEnabled) {
                Log.w(
                    LOG_TAG,
                    "Logging interceptor is enabled. Make sure to disable debugging for release builds!"
                )
                addInterceptor(httpLoggingInterceptor)
            }
        }
        .connectTimeout(connectionTimeoutInMs.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(connectionTimeoutInMs.toLong(), TimeUnit.MILLISECONDS)
        .writeTimeout(connectionTimeoutInMs.toLong(), TimeUnit.MILLISECONDS)
        .build()

    private fun createSSLSocketFactory(trustManagers: Array<TrustManager>?): SSLSocketFactory? {
        return try {
            val sslContext: SSLContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Since Android 10 (Q) TLSv1.3 is default
                // https://developer.android.com/reference/javax/net/ssl/SSLSocket#default-configuration-for-different-android-versions
                // We still need to set it explicitly to be able to call init() on the SSLContext instance
                SSLContext.getInstance("TLSv1.3")
            } else {
                // Force TLSv1.2 on older versions
                SSLContext.getInstance("TLSv1.2")
            }
            sslContext.init(null, trustManagers, null)
            sslContext.socketFactory
        } catch (ignore: NoSuchAlgorithmException) {
            null
        } catch (ignore: KeyManagementException) {
            null
        }
    }

    private fun getTrustManagers(): Array<TrustManager>? {
        if (trustManager != null) {
            return arrayOf(trustManager)
        }
        val pubKeyManager = createPubKeyManager()
        return if (pubKeyManager != null) {
            arrayOf(pubKeyManager)
        } else null
    }

    private fun createPubKeyManager(): PubKeyManager? {
        val builder = PubKeyManager.builder(context)
        if (hostnames.isNotEmpty()) {
            builder.setHostnames(hostnames)
        }
        if (networkSecurityConfigResId != 0) {
            builder.setNetworkSecurityConfigResId(networkSecurityConfigResId)
        }
        return if (builder.canBuild()) {
            builder.build()
        } else null
    }

    /**
     * Builder for configuring and creating [DefaultGiniHttpClientProvider] instances.
     *
     * @param context Android context
     */
    class Builder(private val context: Context) {
        private val hostnames = mutableListOf<String>()
        @XmlRes
        private var networkSecurityConfigResId: Int = 0
        private var cache: Cache? = null
        private var trustManager: TrustManager? = null
        private var connectionTimeoutInMs: Int = DEFAULT_TIMEOUT_MS
        private var isDebuggingEnabled = false

        /**
         * Set the hostnames for certificate pinning.
         *
         * @param hostnames List of hostnames
         * @return This builder instance for chaining
         */
        fun setHostnames(hostnames: List<String>): Builder {
            this.hostnames.clear()
            this.hostnames.addAll(hostnames)
            return this
        }

        /**
         * Set the resource ID for the network security configuration XML to enable public key pinning.
         *
         * @param networkSecurityConfigResId XML resource ID
         * @return This builder instance for chaining
         */
        fun setNetworkSecurityConfigResId(@XmlRes networkSecurityConfigResId: Int): Builder {
            this.networkSecurityConfigResId = networkSecurityConfigResId
            return this
        }

        /**
         * Set the cache implementation to use with OkHttp caching.
         * If no cache is set, no caching will be used.
         *
         * @param cache A cache instance (specified by [okhttp3.Cache])
         * @return This builder instance for chaining
         */
        fun setCache(cache: Cache): Builder {
            this.cache = cache
            return this
        }

        /**
         * Set a custom [TrustManager] implementation to have full control over which certificates to trust.
         *
         * Please be aware that if you set a custom TrustManager implementation here then it will override any
         * [network security configuration](https://developer.android.com/training/articles/security-config)
         * you may have set.
         *
         * @param trustManager A [TrustManager] implementation
         * @return This builder instance for chaining
         */
        fun setTrustManager(trustManager: TrustManager): Builder {
            this.trustManager = trustManager
            return this
        }

        /**
         * Set the connection timeout in milliseconds.
         * This timeout applies to connect, read, and write operations.
         *
         * @param timeoutInMs Timeout in milliseconds (must be >= 0)
         * @return This builder instance for chaining
         * @throws IllegalArgumentException if timeout is negative
         */
        fun setConnectionTimeoutInMs(timeoutInMs: Int): Builder {
            require(timeoutInMs >= 0) { "connectionTimeoutInMs can't be less than 0" }
            this.connectionTimeoutInMs = timeoutInMs
            return this
        }

        /**
         * Enable or disable debugging.
         *
         * Disabled by default.
         *
         * When enabled, all HTTP requests and responses are logged.
         *
         * **WARNING**: Make sure to disable debugging for release builds.
         *
         * @param enabled Pass `true` to enable and `false` to disable debugging
         * @return This builder instance for chaining
         */
        fun setDebuggingEnabled(enabled: Boolean): Builder {
            isDebuggingEnabled = enabled
            if (isDebuggingEnabled) {
                Log.w(LOG_TAG, "Debugging enabled. Make sure to disable debugging for release builds!")
            }
            return this
        }

        /**
         * Build the [DefaultGiniHttpClientProvider] instance with the configured settings.
         *
         * @return A new [DefaultGiniHttpClientProvider] instance
         */
        fun build(): DefaultGiniHttpClientProvider {
            return DefaultGiniHttpClientProvider(
                context = context,
                hostnames = hostnames.toList(),
                networkSecurityConfigResId = networkSecurityConfigResId,
                cache = cache,
                trustManager = trustManager,
                connectionTimeoutInMs = connectionTimeoutInMs,
                isDebuggingEnabled = isDebuggingEnabled
            )
        }
    }

    companion object {
        private const val LOG_TAG = "DefaultGiniHttpClientProvider"
        private const val DEFAULT_TIMEOUT_MS = 60_000
        private val FALLBACK_USER_AGENT =
            "okhttp/${okhttp3.OkHttp.VERSION} (Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID})"

        /**
         * Create a new builder for configuring a [DefaultGiniHttpClientProvider].
         *
         * @param context Android context
         * @return A new [Builder] instance
         */
        @JvmStatic
        fun builder(context: Context): Builder {
            return Builder(context)
        }
    }
}
