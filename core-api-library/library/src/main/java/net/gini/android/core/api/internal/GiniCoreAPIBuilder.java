package net.gini.android.core.api.internal;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.squareup.moshi.Moshi;

import net.gini.android.core.api.ApiCommunicator;
import net.gini.android.core.api.DocumentManager;
import net.gini.android.core.api.DocumentTaskManager;
import net.gini.android.core.api.GiniApiType;
import net.gini.android.core.api.RequestQueueBuilder;
import net.gini.android.core.api.authorization.AnonymousSessionManager;
import net.gini.android.core.api.authorization.CredentialsStore;
import net.gini.android.core.api.authorization.EncryptedCredentialsStore;
import net.gini.android.core.api.authorization.KAnonymousSessionManager;
import net.gini.android.core.api.authorization.SessionManager;
import net.gini.android.core.api.authorization.UserCenterAPICommunicator;
import net.gini.android.core.api.authorization.UserRemoteSource;
import net.gini.android.core.api.authorization.UserRepository;
import net.gini.android.core.api.authorization.UserService;
import net.gini.android.core.api.models.ExtractionsContainer;
import net.gini.android.core.api.requests.DefaultRetryPolicyFactory;
import net.gini.android.core.api.requests.RetryPolicyFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.TrustManager;

import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;
import kotlinx.coroutines.GlobalScope;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

import static net.gini.android.core.api.Utils.checkNotNull;

public abstract class GiniCoreAPIBuilder<DTM extends DocumentTaskManager<A, E>, DM extends DocumentManager<A, DTM, E>, G extends GiniCoreAPI<DTM, DM, A, E>, A extends ApiCommunicator, E extends ExtractionsContainer> {

    private final Context mContext;

    private String mApiBaseUrl;
    private String mUserCenterApiBaseUrl = "https://user.gini.net/";

    private String mEmailDomain;
    private String mClientId;
    private String mClientSecret;
    @XmlRes
    private int mNetworkSecurityConfigResId;

    private A mApiCommunicator;
    private Moshi mMoshi;
    private RequestQueue mRequestQueue;
    private DTM mDocumentTaskManager;
    private SessionManager mSessionManager;
    private CredentialsStore mCredentialsStore;
    private UserCenterAPICommunicator mUserCenterApiCommunicator;
    private int mTimeoutInMs = DefaultRetryPolicy.DEFAULT_TIMEOUT_MS;
    private int mMaxRetries = DefaultRetryPolicy.DEFAULT_MAX_RETRIES;
    private float mBackOffMultiplier = DefaultRetryPolicy.DEFAULT_BACKOFF_MULT;
    private RetryPolicyFactory mRetryPolicyFactory;
    private Cache mCache;
    private TrustManager mTrustManager;
    private Retrofit mUserApiRetrofit;
    private Retrofit mPayApiRetrofit;
    private UserService mUserService;
    private UserRepository mUserRepository;
    private UserRemoteSource mUserRemoteSource;

    /**
     * Constructor to initialize a new builder instance where anonymous Gini users are used. <b>This requires access to
     * the Gini User Center API. Access to the User Center API is restricted to selected clients only.</b>
     *
     * @param context      Your application's Context instance (Android).
     * @param clientId     Your application's client ID for the Gini API.
     * @param clientSecret Your application's client secret for the Gini API.
     * @param emailDomain  The email domain which is used for created Gini users.
     */
    protected GiniCoreAPIBuilder(@NonNull final Context context, @NonNull final String clientId,
                              @NonNull final String clientSecret, @NonNull final String emailDomain) {
        mContext = context;
        mEmailDomain = emailDomain;
        mClientSecret = clientSecret;
        mClientId = clientId;
    }

    /**
     * Constructor to initialize a new builder instance. The created Gini instance will use the given
     * {@link SessionManager} for session management.
     *
     * @param context        Your application's Context instance (Android).
     * @param sessionManager The SessionManager to use.
     */
    protected GiniCoreAPIBuilder(@NonNull final Context context, @NonNull final SessionManager sessionManager) {
        mContext = context;
        mSessionManager = sessionManager;
    }

    /**
     * Set the resource id for the network security configuration xml to enable public key pinning.
     *
     * @param networkSecurityConfigResId xml resource id
     * @return The builder instance to enable chaining.
     */
    public GiniCoreAPIBuilder<DTM, DM, G, A, E> setNetworkSecurityConfigResId(@XmlRes final int networkSecurityConfigResId) {
        mNetworkSecurityConfigResId = networkSecurityConfigResId;
        return this;
    }

    /**
     * Set the base URL of the Gini API. Handy for tests. <b>Usually, you do not use this method</b>.
     *
     * @param newUrl The URL of the Gini API which is used by the requests of the library.
     * @return The builder instance to enable chaining.
     */
    public GiniCoreAPIBuilder<DTM, DM, G, A, E> setApiBaseUrl(@NonNull String newUrl) {
        if (!newUrl.endsWith("/")) {
            newUrl += "/";
        }
        mApiBaseUrl = newUrl;
        return this;
    }

    /**
     * Set the base URL of the Gini User Center API. Handy for tests. <b>Usually, you do not use this method</b>.
     *
     * @param newUrl The URL of the Gini User Center API which is used by the requests of the library.
     * @return The builder instance to enable chaining.
     */
    public GiniCoreAPIBuilder<DTM, DM, G, A, E> setUserCenterApiBaseUrl(@NonNull String newUrl) {
        if (!newUrl.endsWith("/")) {
            newUrl += "/";
        }
        mUserCenterApiBaseUrl = newUrl;
        return this;
    }

    @NonNull
    public abstract GiniApiType getGiniApiType();

    /**
     * Sets the (initial) timeout for each request. A timeout error will occur if nothing is received from the underlying socket in the given time span.
     * The initial timeout will be altered depending on the #backoffMultiplier and failed retries.
     *
     * @param connectionTimeoutInMs initial timeout
     * @return The builder instance to enable chaining.
     */
    public GiniCoreAPIBuilder<DTM, DM, G, A, E> setConnectionTimeoutInMs(final int connectionTimeoutInMs) {
        if (connectionTimeoutInMs < 0) {
            throw new IllegalArgumentException("connectionTimeoutInMs can't be less than 0");
        }
        mTimeoutInMs = connectionTimeoutInMs;
        return this;
    }

    /**
     * Sets the maximal number of retries for each network request.
     *
     * @param maxNumberOfRetries maximal number of retries.
     * @return The builder instance to enable chaining.
     */
    public GiniCoreAPIBuilder<DTM, DM, G, A, E> setMaxNumberOfRetries(final int maxNumberOfRetries) {
        if (maxNumberOfRetries < 0) {
            throw new IllegalArgumentException("maxNumberOfRetries can't be less than 0");
        }
        mMaxRetries = maxNumberOfRetries;
        return this;
    }

    /**
     * Sets the backoff multiplication factor for connection retries.
     * In case of failed retries the timeout of the last request attempt is multiplied with this factor
     *
     * @param backOffMultiplier the backoff multiplication factor
     * @return The builder instance to enable chaining.
     */
    public GiniCoreAPIBuilder<DTM, DM, G, A, E> setConnectionBackOffMultiplier(final float backOffMultiplier) {
        if (backOffMultiplier < 0.0) {
            throw new IllegalArgumentException("backOffMultiplier can't be less than 0");
        }
        mBackOffMultiplier = backOffMultiplier;
        return this;
    }

    /**
     * Set the credentials store which is used by the library to store user credentials. If no credentials store is
     * set, the net.gini.android.core.api.authorization.SharedPreferencesCredentialsStore is used by default.
     *
     * @param credentialsStore A credentials store instance (specified by the CredentialsStore interface).
     * @return The builder instance to enable chaining.
     */
    public GiniCoreAPIBuilder<DTM, DM, G, A, E> setCredentialsStore(@NonNull CredentialsStore credentialsStore) {
        mCredentialsStore = checkNotNull(credentialsStore);
        return this;
    }

    /**
     * Set the cache implementation to use with Volley. If no cache is set, the default Volley cache
     * will be used.
     *
     * @param cache A cache instance (specified by the com.android.volley.Cache interface).
     * @return The builder instance to enable chaining.
     */
    public GiniCoreAPIBuilder<DTM, DM, G, A, E> setCache(@NonNull Cache cache) {
        mCache = cache;
        return this;
    }

    /**
     * Set a custom {@link TrustManager} implementation to have full control over which certificates to trust.
     * <p>
     * Please be aware that if you set a custom TrustManager implementation here than it will override any
     * <a href="https://developer.android.com/training/articles/security-config">network security configuration</a>
     * you may have set.
     *
     * @param trustManager A {@link TrustManager} implementation.
     * @return The builder instance to enable chaining.
     */
    public GiniCoreAPIBuilder<DTM, DM, G, A, E> setTrustManager(@NonNull final TrustManager trustManager) {
        mTrustManager = trustManager;
        return this;
    }

    /**
     * Builds an instance with the configuration settings of the builder instance.
     *
     * @return The fully configured instance.
     */
    public abstract G build();

    /**
     * Helper method to create (and store) the RequestQueue which is used for both the requests to the Gini API and the
     * Gini User Center API.
     *
     * @return The RequestQueue instance.
     */
    @NonNull
    protected synchronized RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            RequestQueueBuilder requestQueueBuilder = new RequestQueueBuilder(mContext);
            requestQueueBuilder.setHostnames(getHostnames());
            if (mCache != null) {
                requestQueueBuilder.setCache(mCache);
            }
            if (mNetworkSecurityConfigResId != 0) {
                requestQueueBuilder.setNetworkSecurityConfigResId(mNetworkSecurityConfigResId);
            } else if (mTrustManager != null) {
                requestQueueBuilder.setTrustManager(mTrustManager);
            }
            mRequestQueue = requestQueueBuilder.build();
        }
        return mRequestQueue;
    }

    protected String getApiBaseUrl() {
        return mApiBaseUrl != null ? mApiBaseUrl : getGiniApiType().getBaseUrl();
    }

    @NonNull
    private List<String> getHostnames() {
        final List<String> hostnames = new ArrayList<>(2);
        try {
            hostnames.add(new URL(getApiBaseUrl()).getHost());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Gini API base url", e);
        }
        try {
            hostnames.add(new URL(mUserCenterApiBaseUrl).getHost());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Gini API base url", e);
        }
        return hostnames;
    }

    /**
     * Helper method to create (and store) the ApiCommunicator instance which is used to do the requests to the Gini API.
     *
     * @return The ApiCommunicator instance.
     */
    @NonNull
    protected synchronized A getApiCommunicator() {
        if (mApiCommunicator == null) {
            mApiCommunicator = createApiCommunicator();
        }
        return mApiCommunicator;
    }

    protected abstract A createApiCommunicator();

    /**
     * Helper method to create (and store) the ApiCommunicator instance which is used to do the requests to the Gini API.
     *
     * @return The ApiCommunicator instance.
     */
    @NonNull
    protected synchronized Moshi getMoshi() {
        if (mMoshi == null) {
            mMoshi = new Moshi.Builder().build();
        }
        return mMoshi;
    }

    /**
     * Helper method to create (and store) the instance of the CredentialsStore implementation which is used to store
     * user credentials. If the credentials store was previously configured via the builder, the previously configured
     * instance is used. Otherwise, a net.gini.android.core.api.authorization.EncryptedCredentialsStore instance is
     * created by default.
     *
     * @return The CredentialsStore instance.
     */
    @NonNull
    protected synchronized CredentialsStore getCredentialsStore() {
        if (mCredentialsStore == null) {
            SharedPreferences sharedPreferences = mContext.getSharedPreferences("Gini",
                    Context.MODE_PRIVATE);
            final EncryptedCredentialsStore encryptedCredentialsStore = new EncryptedCredentialsStore(
                    sharedPreferences, mContext);
            encryptedCredentialsStore.encryptExistingPlaintextCredentials();
            mCredentialsStore = encryptedCredentialsStore;
        }
        return mCredentialsStore;
    }

    /**
     * Helper method to create (and store) the UserRepository instance which is used to do the requests to
     * the Gini User Center API.
     *
     * @return The UserRepository instance.
     */
    @NonNull
    private synchronized UserRepository getUserRepository() {
        if (mUserRepository == null) {
            mUserRepository = new UserRepository(getmUserRemoteSource().getCoroutineContext(), null, getmUserRemoteSource());
//                    new UserRepository(getRequestQueue(), mUserCenterApiBaseUrl,
//                            getGiniApiType(), mClientId, mClientSecret,
//                            getRetryPolicyFactory());
        }
        return mUserRepository;
    }

    /**
     * Helper method to create a {@link RetryPolicyFactory} instance which is used to create a
     * {@link com.android.volley.RetryPolicy} for each request.
     *
     * @return The RetryPolicyFactory instance.
     */
    @NonNull
    protected synchronized RetryPolicyFactory getRetryPolicyFactory() {
        if (mRetryPolicyFactory == null) {
            mRetryPolicyFactory = new DefaultRetryPolicyFactory(mTimeoutInMs, mMaxRetries,
                    mBackOffMultiplier);
        }
        return mRetryPolicyFactory;
    }

    /**
     * Helper method to create a DocumentTaskManager instance.
     *
     * @return The DocumentTaskManager instance.
     */
    @NonNull
    protected synchronized DTM getDocumentTaskManager() {
        if (mDocumentTaskManager == null) {
            mDocumentTaskManager = createDocumentTaskManager();
        }
        return mDocumentTaskManager;
    }

    protected abstract DTM createDocumentTaskManager();

    /**
     * Return the {@link SessionManager} set via #setSessionManager. If no SessionManager has been set, default to
     * {@link AnonymousSessionManager}.
     *
     * @return The SessionManager instance.
     */
    @NonNull
    public synchronized SessionManager getSessionManager() {
        if (mSessionManager == null) {
            mSessionManager = new KAnonymousSessionManager(mEmailDomain, getUserCenterManager(),
                    getCredentialsStore());
        }
        return mSessionManager;
    }

    private synchronized Retrofit getUserApiRetrofit() {
        mUserApiRetrofit = new Retrofit.Builder()
                .baseUrl(mUserCenterApiBaseUrl)
                .addConverterFactory(MoshiConverterFactory.create())
                .client(new OkHttpClient.Builder()
                        .connectTimeout(mTimeoutInMs, TimeUnit.MILLISECONDS)
                        .readTimeout(mTimeoutInMs, TimeUnit.MILLISECONDS)
                        .writeTimeout(mTimeoutInMs, TimeUnit.MILLISECONDS).build())
                .build();
        return mUserApiRetrofit;
    }

    private synchronized Retrofit getPayApiRetrofit() {
        mPayApiRetrofit = new Retrofit.Builder()
                .baseUrl(getApiBaseUrl())
                .addConverterFactory(MoshiConverterFactory.create())
                .client(new OkHttpClient.Builder()
                        .connectTimeout(mTimeoutInMs, TimeUnit.MILLISECONDS)
                        .readTimeout(mTimeoutInMs, TimeUnit.MILLISECONDS)
                        .writeTimeout(mTimeoutInMs, TimeUnit.MILLISECONDS).build())
                .build();
        return  mPayApiRetrofit;
    }

    protected synchronized UserService getmUserService() {
        if (mUserService == null) {
            mUserService = getUserApiRetrofit().create(UserService.class);
        }
        return mUserService;
    }

    protected synchronized UserRemoteSource getmUserRemoteSource() {
        if (mUserRemoteSource == null) {
            mUserRemoteSource = new UserRemoteSource(GlobalScope.INSTANCE.getCoroutineContext(), getmUserService());
        }

        return  mUserRemoteSource;
    }
}

