package net.gini.android.core.api.internal

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.XmlRes
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import net.gini.android.core.api.BuildConfig
import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.Utils
import net.gini.android.core.api.authorization.AnonymousSessionManager
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.authorization.EncryptedCredentialsStore
import net.gini.android.core.api.authorization.PubKeyManager
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.authorization.UserRemoteSource
import net.gini.android.core.api.authorization.UserRepository
import net.gini.android.core.api.authorization.UserService
import net.gini.android.core.api.authorization.X509TrustManagerAdapter
import net.gini.android.core.api.models.ExtractionsContainer
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.MalformedURLException
import java.net.URL
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager

/**
 * The [GiniCoreAPIBuilder] allows you to build and configure a [GiniCoreAPI] instance.
 *
 * @constructor Initializes a new builder instance.
 * @param context your application's Context instance (Android)
 * @param clientId your application's client ID for the Gini Health API
 * @param clientSecret your application's client secret for the Gini Health API
 * @param emailDomain  the email domain which is used for created Gini users
 * @param sessionManager if not null, then the [SessionManager] instance will be used for session management. If null, then anonymous Gini users will be used.
 */
abstract class GiniCoreAPIBuilder<DM : DocumentManager<DR, E>, G : GiniCoreAPI<DM,DR, E>, DR : DocumentRepository<E>, E : ExtractionsContainer>(
    private val context: Context,
    private val clientId: String,
    private val clientSecret: String,
    private val emailDomain: String,
    private var sessionManager: SessionManager? = null
) {
    private var mApiBaseUrl: String? = null
    private var mUserCenterApiBaseUrl = "https://user.gini.net/"
    @XmlRes
    private var mNetworkSecurityConfigResId = 0
    private var mMoshi: Moshi? = null
    private var mCredentialsStore: CredentialsStore? = null
    private var mTimeoutInMs = 60_000
    private var mCache: Cache? = null
    private var mTrustManager: TrustManager? = null
    private var mUserApiRetrofit: Retrofit? = null
    private var mPayApiRetrofit: Retrofit? = null
    private var mUserService: UserService? = null
    private var mUserRepository: UserRepository? = null
    private var mUserRemoteSource: UserRemoteSource? = null
    private var mDocumentManager: DM? = null
    private var mDocumentRepository: DR? = null
    private var isDebuggingEnabled = false
    private val httpLoggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    /**
     * Set the resource id for the network security configuration xml to enable public key pinning.
     *
     * @param networkSecurityConfigResId xml resource id
     * @return The builder instance to enable chaining.
     */
    open fun setNetworkSecurityConfigResId(@XmlRes networkSecurityConfigResId: Int): GiniCoreAPIBuilder<DM, G, DR, E> {
        mNetworkSecurityConfigResId = networkSecurityConfigResId
        return this
    }

    /**
     * Set the base URL of the Gini API. Handy for tests. **Usually, you do not use this method**.
     *
     * @param newUrl The URL of the Gini API which is used by the requests of the library.
     * @return The builder instance to enable chaining.
     */
    open fun setApiBaseUrl(newUrl: String): GiniCoreAPIBuilder<DM, G, DR, E> {
        var baseUrl = newUrl
        if (!newUrl.endsWith("/")) {
            baseUrl += "/"
        }
        mApiBaseUrl = baseUrl
        return this
    }

    /**
     * Set the base URL of the Gini User Center API. Handy for tests. **Usually, you do not use this method**.
     *
     * @param newUrl The URL of the Gini User Center API which is used by the requests of the library.
     * @return The builder instance to enable chaining.
     */
    open fun setUserCenterApiBaseUrl(newUrl: String): GiniCoreAPIBuilder<DM, G, DR, E> {
        var baseUrl = newUrl
        if (!newUrl.endsWith("/")) {
            baseUrl += "/"
        }
        mUserCenterApiBaseUrl = baseUrl
        return this
    }

    /**
     * Differentiate between different types of GiniApi's.
     */
    abstract fun getGiniApiType(): GiniApiType

    /**
     * Sets the (initial) timeout for each request. A timeout error will occur if nothing is received from the underlying socket in the given time span.
     * The initial timeout will be altered depending on the #backoffMultiplier and failed retries.
     *
     * @param connectionTimeoutInMs initial timeout
     * @return The builder instance to enable chaining.
     */
    open fun setConnectionTimeoutInMs(connectionTimeoutInMs: Int): GiniCoreAPIBuilder<DM, G, DR, E> {
        require(connectionTimeoutInMs >= 0) { "connectionTimeoutInMs can't be less than 0" }
        mTimeoutInMs = connectionTimeoutInMs
        return this
    }

    /**
     * Set the credentials store which is used by the library to store user credentials. If no credentials store is
     * set, the net.gini.android.core.api.authorization.SharedPreferencesCredentialsStore is used by default.
     *
     * @param credentialsStore A credentials store instance (specified by the CredentialsStore interface).
     * @return The builder instance to enable chaining.
     */
    open fun setCredentialsStore(credentialsStore: CredentialsStore): GiniCoreAPIBuilder<DM, G, DR, E> {
        mCredentialsStore = Utils.checkNotNull(credentialsStore)
        return this
    }

    /**
     * Set the cache implementation to use with Volley. If no cache is set, the default Volley cache
     * will be used.
     *
     * @param cache A cache instance (specified by the com.android.volley.Cache interface).
     * @return The builder instance to enable chaining.
     */
    open fun setCache(cache: Cache): GiniCoreAPIBuilder<DM, G, DR, E> {
        mCache = cache
        return this
    }

    /**
     * Set a custom [TrustManager] implementation to have full control over which certificates to trust.
     *
     *
     * Please be aware that if you set a custom TrustManager implementation here than it will override any
     * [network security configuration](https://developer.android.com/training/articles/security-config)
     * you may have set.
     *
     * @param trustManager A [TrustManager] implementation.
     * @return The builder instance to enable chaining.
     */
    open fun setTrustManager(trustManager: TrustManager): GiniCoreAPIBuilder<DM, G, DR, E> {
        mTrustManager = trustManager
        return this
    }

    /**
     * Enable or disable debugging.
     *
     * Disabled by default.
     *
     * When enabled all the requests and responses are logged.
     *
     * WARNING: Make sure to disable debugging for release builds.
     *
     * @param enabled pass `true` to enable and `false` to disable debugging
     * @return The builder instance to enable chaining.
     */
    fun setDebuggingEnabled(enabled: Boolean): GiniCoreAPIBuilder<DM, G, DR, E> {
        isDebuggingEnabled = enabled
        if (isDebuggingEnabled) {
            Log.w(LOG_TAG, "Debugging enabled. Make sure to disable debugging for release builds!")
        }
        return this
    }

    /**
     * Builds an instance with the configuration settings of the builder instance.
     *
     * @return The fully configured instance.
     */
    abstract fun build(): G

    protected fun getApiBaseUrl(): String? {
        return if (mApiBaseUrl != null) mApiBaseUrl else getGiniApiType().baseUrl
    }

    private fun getHostnames(): List<String> {
        val hostnames: MutableList<String> = ArrayList(2)
        try {
            hostnames.add(URL(getApiBaseUrl()).host)
        } catch (e: MalformedURLException) {
            throw RuntimeException("Invalid Gini API base url", e)
        }
        try {
            hostnames.add(URL(mUserCenterApiBaseUrl).host)
        } catch (e: MalformedURLException) {
            throw RuntimeException("Invalid Gini API base url", e)
        }
        return hostnames
    }

    /**
     * Helper method to create (and store) the ApiCommunicator instance which is used to do the requests to the Gini API.
     *
     * @return The ApiCommunicator instance.
     */
    @Synchronized
    protected fun getMoshi(): Moshi {
        if (mMoshi == null) {
            mMoshi = Moshi.Builder().build()
        }
        return mMoshi!!
    }

    /**
     * Helper method to create (and store) the instance of the CredentialsStore implementation which is used to store
     * user credentials. If the credentials store was previously configured via the builder, the previously configured
     * instance is used. Otherwise, a net.gini.android.core.api.authorization.EncryptedCredentialsStore instance is
     * created by default.
     *
     * @return The CredentialsStore instance.
     */
    @Synchronized
    protected fun getCredentialsStore(): CredentialsStore {
        if (mCredentialsStore == null) {
            val sharedPreferences: SharedPreferences = context.getSharedPreferences("Gini", Context.MODE_PRIVATE)
            val encryptedCredentialsStore = EncryptedCredentialsStore(sharedPreferences, context)
            encryptedCredentialsStore.encryptExistingPlaintextCredentials()
            mCredentialsStore = encryptedCredentialsStore
        }
        return mCredentialsStore as CredentialsStore
    }

    /**
     * Helper method to create (and store) the UserRepository instance which is used to do the requests to
     * the Gini User Center API.
     *
     * @return The UserRepository instance.
     */
    @Synchronized
    private fun getUserRepository(): UserRepository {
        if (mUserRepository == null) {
            mUserRepository = UserRepository(getUserRemoteSource())
        }
        return mUserRepository as UserRepository
    }

    /**
     * Helper method to create a DocumentTaskManager instance.
     *
     * @return The DocumentTaskManager instance.
     */
    @Synchronized
    protected fun getDocumentManager(): DM {
        if (mDocumentManager == null) {
            mDocumentManager = createDocumentManager()
        }
        return mDocumentManager as DM
    }

    protected abstract fun createDocumentManager(): DM

    protected abstract fun createDocumentRepository(): DR

    /**
     * Return the [SessionManager] set via #setSessionManager. If no SessionManager has been set, default to
     * [AnonymousSessionManager].
     *
     * @return The SessionManager instance.
     */
    @Synchronized
    open fun getSessionManager(): SessionManager {
        if (sessionManager == null) {
            sessionManager = AnonymousSessionManager(getUserRepository(), getCredentialsStore(), emailDomain)
        }
        return sessionManager as SessionManager
    }

    @Synchronized
    private fun getUserApiRetrofit(): Retrofit {
        val retrofit = Retrofit.Builder()
            .baseUrl(mUserCenterApiBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(getMoshi()))
            .client(createOkHttpClient())
            .build()
        mUserApiRetrofit = retrofit
        return retrofit
    }

    private fun createOkHttpClient() = OkHttpClient.Builder()
        .apply {
            // Set system user agent string or fallback user agent if it's not available
            addInterceptor { chain ->
                chain.proceed(
                    chain.request()
                        .newBuilder()
                        .header("User-Agent", System.getProperty("http.agent") ?: FALLBACK_USER_AGENT)
                        .build()
                )
            }

           getTrustManagers()?.let { trustManagers ->
                createSSLSocketFactory(trustManagers)?.let { socketFactory ->
                    sslSocketFactory(socketFactory, X509TrustManagerAdapter(trustManagers[0]))
                }
            }

            if (mCache != null) {
                cache(mCache)
            }

            if (isDebuggingEnabled && BuildConfig.DEBUG) {
                Log.w(LOG_TAG, "Logging interceptor is enabled. Make sure to disable debugging for release builds!")
                addInterceptor(httpLoggingInterceptor)
            }
        }
        .connectTimeout(mTimeoutInMs.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(mTimeoutInMs.toLong(), TimeUnit.MILLISECONDS)
        .writeTimeout(mTimeoutInMs.toLong(), TimeUnit.MILLISECONDS)
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
        if (mTrustManager != null) {
            return arrayOf(mTrustManager!!)
        }
        val pubKeyManager = createPubKeyManager()
        return if (pubKeyManager != null) {
            arrayOf(pubKeyManager)
        } else null
    }

    private fun createPubKeyManager(): PubKeyManager? {
        val builder = PubKeyManager.builder(context)
        val hostnames = getHostnames();
        if (hostnames.isNotEmpty()) {
            builder.setHostnames(hostnames)
        }
        if (mNetworkSecurityConfigResId != 0) {
            builder.setNetworkSecurityConfigResId(mNetworkSecurityConfigResId)
        }
        return if (builder.canBuild()) {
            builder.build()
        } else null
    }

    @Synchronized
    protected fun getApiRetrofit(): Retrofit {
        val retrofit = Retrofit.Builder()
            .baseUrl(getApiBaseUrl()!!)
            .addConverterFactory(MoshiConverterFactory.create(getMoshi()))
            .client(createOkHttpClient())
            .build()
        mPayApiRetrofit = retrofit
        return retrofit
    }

    @Synchronized
    private fun getUserService(): UserService? {
        if (mUserService == null) {
            mUserService = getUserApiRetrofit().create(UserService::class.java)
        }
        return mUserService
    }

    @Synchronized
    private fun getUserRemoteSource(): UserRemoteSource {
        if (mUserRemoteSource == null) {
            mUserRemoteSource = UserRemoteSource(Dispatchers.IO, getUserService()!!, clientId, clientSecret)
        }
        return mUserRemoteSource as UserRemoteSource
    }

    protected fun getDocumentRepository(): DR {
        if (mDocumentRepository == null) {
            mDocumentRepository = createDocumentRepository()
        }
        return mDocumentRepository as DR
    }

    companion object {
        const val LOG_TAG = "GiniCoreAPIBuilder"
        val FALLBACK_USER_AGENT = "okhttp/${okhttp3.OkHttp.VERSION} (Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID})"
    }
}
