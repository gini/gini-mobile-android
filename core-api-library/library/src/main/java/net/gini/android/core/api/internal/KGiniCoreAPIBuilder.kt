package net.gini.android.core.api.internal

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.XmlRes
import com.android.volley.Cache
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import net.gini.android.core.api.*
import net.gini.android.core.api.authorization.*
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.requests.DefaultRetryPolicyFactory
import net.gini.android.core.api.requests.RetryPolicyFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManager

abstract class KGiniCoreAPIBuilder<DM : DocumentManager<DR, E>, G : KGiniCoreAPI<DM,DR, E>, DR : DocumentRepository<E>, E : ExtractionsContainer>(
    private val context: Context,
    private val clientId: String,
    private val clientSecret: String,
    private val emailDomain: String,
    private var sessionManager: KSessionManager? = null
) {
    private var mApiBaseUrl: String? = null
    private var mUserCenterApiBaseUrl = "https://user.gini.net/"
    @XmlRes
    private var mNetworkSecurityConfigResId = 0
    private var mMoshi: Moshi? = null
    private var mRequestQueue: RequestQueue? = null
    private var mCredentialsStore: CredentialsStore? = null
    private var mTimeoutInMs = DefaultRetryPolicy.DEFAULT_TIMEOUT_MS
    private var mMaxRetries = DefaultRetryPolicy.DEFAULT_MAX_RETRIES
    private var mBackOffMultiplier = DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    private var mRetryPolicyFactory: RetryPolicyFactory? = null
    private var mCache: Cache? = null
    private var mTrustManager: TrustManager? = null
    private var mUserApiRetrofit: Retrofit? = null
    private var mPayApiRetrofit: Retrofit? = null
    private var mUserService: UserService? = null
    private var mUserRepository: UserRepository? = null
    private var mUserRemoteSource: UserRemoteSource? = null
    private var mDocumentManager: DM? = null
    private var mDocumentRepository: DR? = null

    /**
     * Set the resource id for the network security configuration xml to enable public key pinning.
     *
     * @param networkSecurityConfigResId xml resource id
     * @return The builder instance to enable chaining.
     */
    open fun setNetworkSecurityConfigResId(@XmlRes networkSecurityConfigResId: Int): KGiniCoreAPIBuilder<DM, G, DR, E>? {
        mNetworkSecurityConfigResId = networkSecurityConfigResId
        return this
    }

    /**
     * Set the base URL of the Gini API. Handy for tests. **Usually, you do not use this method**.
     *
     * @param newUrl The URL of the Gini API which is used by the requests of the library.
     * @return The builder instance to enable chaining.
     */
    open fun setApiBaseUrl(newUrl: String): KGiniCoreAPIBuilder<DM, G, DR, E>? {
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
    open fun setUserCenterApiBaseUrl(newUrl: String): KGiniCoreAPIBuilder<DM, G, DR, E>? {
        var baseUrl = newUrl
        if (!newUrl.endsWith("/")) {
            baseUrl += "/"
        }
        mUserCenterApiBaseUrl = baseUrl
        return this
    }

    abstract fun getGiniApiType(): GiniApiType

    /**
     * Sets the (initial) timeout for each request. A timeout error will occur if nothing is received from the underlying socket in the given time span.
     * The initial timeout will be altered depending on the #backoffMultiplier and failed retries.
     *
     * @param connectionTimeoutInMs initial timeout
     * @return The builder instance to enable chaining.
     */
    open fun setConnectionTimeoutInMs(connectionTimeoutInMs: Int): KGiniCoreAPIBuilder<DM, G, DR, E>? {
        require(connectionTimeoutInMs >= 0) { "connectionTimeoutInMs can't be less than 0" }
        mTimeoutInMs = connectionTimeoutInMs
        return this
    }

    /**
     * Sets the maximal number of retries for each network request.
     *
     * @param maxNumberOfRetries maximal number of retries.
     * @return The builder instance to enable chaining.
     */
    open fun setMaxNumberOfRetries(maxNumberOfRetries: Int): KGiniCoreAPIBuilder<DM, G, DR, E>? {
        require(maxNumberOfRetries >= 0) { "maxNumberOfRetries can't be less than 0" }
        mMaxRetries = maxNumberOfRetries
        return this
    }

    /**
     * Sets the backoff multiplication factor for connection retries.
     * In case of failed retries the timeout of the last request attempt is multiplied with this factor
     *
     * @param backOffMultiplier the backoff multiplication factor
     * @return The builder instance to enable chaining.
     */
    open fun setConnectionBackOffMultiplier(backOffMultiplier: Float): KGiniCoreAPIBuilder<DM, G, DR, E>? {
        require(backOffMultiplier >= 0.0) { "backOffMultiplier can't be less than 0" }
        mBackOffMultiplier = backOffMultiplier
        return this
    }

    /**
     * Set the credentials store which is used by the library to store user credentials. If no credentials store is
     * set, the net.gini.android.core.api.authorization.SharedPreferencesCredentialsStore is used by default.
     *
     * @param credentialsStore A credentials store instance (specified by the CredentialsStore interface).
     * @return The builder instance to enable chaining.
     */
    open fun setCredentialsStore(credentialsStore: CredentialsStore): KGiniCoreAPIBuilder<DM, G, DR, E>? {
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
    open fun setCache(cache: Cache): KGiniCoreAPIBuilder<DM, G, DR, E>? {
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
    open fun setTrustManager(trustManager: TrustManager): KGiniCoreAPIBuilder<DM, G, DR, E>? {
        mTrustManager = trustManager
        return this
    }

    /**
     * Builds an instance with the configuration settings of the builder instance.
     *
     * @return The fully configured instance.
     */
    abstract fun build(): G

    /**
     * Helper method to create (and store) the RequestQueue which is used for both the requests to the Gini API and the
     * Gini User Center API.
     *
     * @return The RequestQueue instance.
     */
    @Synchronized
    open fun getRequestQueue(): RequestQueue {
        if (mRequestQueue == null) {
            val requestQueueBuilder = RequestQueueBuilder(context)
            requestQueueBuilder.setHostnames(getHostnames())
            if (mCache != null) {
                requestQueueBuilder.setCache(mCache)
            }
            if (mNetworkSecurityConfigResId != 0) {
                requestQueueBuilder.setNetworkSecurityConfigResId(mNetworkSecurityConfigResId)
            } else if (mTrustManager != null) {
                requestQueueBuilder.setTrustManager(mTrustManager!!)
            }
            mRequestQueue = requestQueueBuilder.build()
        }
        return mRequestQueue!!
    }

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
            mUserRepository =
                getmUserRemoteSource()?.let {
                    getmUserRemoteSource()?.coroutineContext?.let { it1 -> UserRepository(it1, it) }
                }
        }
        return mUserRepository as UserRepository
    }

    /**
     * Helper method to create a [RetryPolicyFactory] instance which is used to create a
     * [com.android.volley.RetryPolicy] for each request.
     *
     * @return The RetryPolicyFactory instance.
     */
    @Synchronized
    protected fun getRetryPolicyFactory(): RetryPolicyFactory {
        if (mRetryPolicyFactory == null) {
            mRetryPolicyFactory = DefaultRetryPolicyFactory(
                mTimeoutInMs, mMaxRetries,
                mBackOffMultiplier
            )
        }
        return mRetryPolicyFactory as RetryPolicyFactory
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
    open fun getSessionManager(): KSessionManager {
        if (sessionManager == null) {
            sessionManager = KAnonymousSessionManager(getUserRepository(), getCredentialsStore(), emailDomain)
        }
        return sessionManager as KSessionManager
    }

    @Synchronized
    private fun getUserApiRetrofit(): Retrofit {
        mUserApiRetrofit = Retrofit.Builder()
            .baseUrl(mUserCenterApiBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(mTimeoutInMs.toLong(), TimeUnit.MILLISECONDS)
                    .readTimeout(mTimeoutInMs.toLong(), TimeUnit.MILLISECONDS)
                    .writeTimeout(mTimeoutInMs.toLong(), TimeUnit.MILLISECONDS).build()
            )
            .build()
        return mUserApiRetrofit as Retrofit
    }

    @Synchronized
    protected fun getApiRetrofit(): Retrofit {
        mPayApiRetrofit = Retrofit.Builder()
            .baseUrl(getApiBaseUrl()!!)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(mTimeoutInMs.toLong(), TimeUnit.MILLISECONDS)
                    .readTimeout(mTimeoutInMs.toLong(), TimeUnit.MILLISECONDS)
                    .writeTimeout(mTimeoutInMs.toLong(), TimeUnit.MILLISECONDS).build()
            )
            .build()
        return mPayApiRetrofit as Retrofit
    }

    @Synchronized
    protected fun getmUserService(): UserService? {
        if (mUserService == null) {
            mUserService = getUserApiRetrofit().create(UserService::class.java)
        }
        return mUserService
    }

    @Synchronized
    protected fun getmUserRemoteSource(): UserRemoteSource? {
        if (mUserRemoteSource == null) {
            mUserRemoteSource = UserRemoteSource(Dispatchers.IO, getmUserService()!!, clientId, clientSecret)
        }
        return mUserRemoteSource
    }

    protected fun getDocumentRepository(): DR {
        if (mDocumentRepository == null) {
            mDocumentRepository = createDocumentRepository()
        }
        return mDocumentRepository as DR
    }
}
