package net.gini.android;

import static net.gini.android.Utils.checkNotNull;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.squareup.moshi.Moshi;

import net.gini.android.authorization.AnonymousSessionManager;
import net.gini.android.authorization.CredentialsStore;
import net.gini.android.authorization.EncryptedCredentialsStore;
import net.gini.android.authorization.SessionManager;
import net.gini.android.authorization.UserCenterAPICommunicator;
import net.gini.android.authorization.UserCenterManager;
import net.gini.android.requests.DefaultRetryPolicyFactory;
import net.gini.android.requests.RetryPolicyFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;

public class GiniBuilder {

    private final Context mContext;

    private String mApiBaseUrl;
    private String mUserCenterApiBaseUrl = "https://user.gini.net/";

    private String mEmailDomain;
    private String mClientId;
    private String mClientSecret;
    @XmlRes
    private int mNetworkSecurityConfigResId;

    private ApiCommunicator mApiCommunicator;
    private Moshi mMoshi;
    private RequestQueue mRequestQueue;
    private DocumentTaskManager mDocumentTaskManager;
    private SessionManager mSessionManager;
    private CredentialsStore mCredentialsStore;
    private UserCenterManager mUserCenterManager;
    private UserCenterAPICommunicator mUserCenterApiCommunicator;
    private int mTimeoutInMs = DefaultRetryPolicy.DEFAULT_TIMEOUT_MS;
    private int mMaxRetries = DefaultRetryPolicy.DEFAULT_MAX_RETRIES;
    private float mBackOffMultiplier = DefaultRetryPolicy.DEFAULT_BACKOFF_MULT;
    private RetryPolicyFactory mRetryPolicyFactory;
    private Cache mCache;
    private GiniApiType mGiniApiType;

    /**
     * Constructor to initialize a new builder instance where anonymous Gini users are used. <b>This requires access to
     * the Gini User Center API. Access to the User Center API is restricted to selected clients only.</b>
     *
     * @param context      Your application's Context instance (Android).
     * @param clientId     Your application's client ID for the Gini API.
     * @param clientSecret Your application's client secret for the Gini API.
     * @param emailDomain  The email domain which is used for created Gini users.
     */
    public GiniBuilder(@NonNull final Context context, @NonNull final String clientId,
                       @NonNull final String clientSecret, @NonNull final String emailDomain) {
        mContext = context;
        mEmailDomain = emailDomain;
        mClientSecret = clientSecret;
        mClientId = clientId;
        mGiniApiType = GiniApiType.DEFAULT;
    }

    /**
     * Constructor to initialize a new builder instance. The created Gini instance will use the given
     * {@link SessionManager} for session management.
     *
     * @param context        Your application's Context instance (Android).
     * @param sessionManager The SessionManager to use.
     */
    public GiniBuilder(@NonNull final Context context, @NonNull final SessionManager sessionManager) {
        mContext = context;
        mSessionManager = sessionManager;
        mGiniApiType = GiniApiType.DEFAULT;
    }

    /**
     * Set the resource id for the network security configuration xml to enable public key pinning.
     *
     * @param networkSecurityConfigResId xml resource id
     * @return The builder instance to enable chaining.
     */
    public GiniBuilder setNetworkSecurityConfigResId(@XmlRes final int networkSecurityConfigResId) {
        mNetworkSecurityConfigResId = networkSecurityConfigResId;
        return this;
    }

    /**
     * Set the base URL of the Gini API. Handy for tests. <b>Usually, you do not use this method</b>.
     *
     * @param newUrl The URL of the Gini API which is used by the requests of the library.
     * @return The builder instance to enable chaining.
     */
    public GiniBuilder setApiBaseUrl(@NonNull String newUrl) {
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
    public GiniBuilder setUserCenterApiBaseUrl(@NonNull String newUrl) {
        if (!newUrl.endsWith("/")) {
            newUrl += "/";
        }
        mUserCenterApiBaseUrl = newUrl;
        return this;
    }

    /**
     * Set which Gini API to use. See {@link GiniApiType} for options.
     *
     * @param giniApiType the {@link GiniApiType} to be used
     *
     * @return The builder instance to enable chaining.
     */
    public GiniBuilder setGiniApiType(@NonNull final GiniApiType giniApiType) {
        mGiniApiType = giniApiType;
        return this;
    }

    /**
     * Sets the (initial) timeout for each request. A timeout error will occur if nothing is received from the underlying socket in the given time span.
     * The initial timeout will be altered depending on the #backoffMultiplier and failed retries.
     *
     * @param connectionTimeoutInMs initial timeout
     * @return The builder instance to enable chaining.
     */
    public GiniBuilder setConnectionTimeoutInMs(final int connectionTimeoutInMs) {
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
    public GiniBuilder setMaxNumberOfRetries(final int maxNumberOfRetries) {
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
    public GiniBuilder setConnectionBackOffMultiplier(final float backOffMultiplier) {
        if (backOffMultiplier < 0.0) {
            throw new IllegalArgumentException("backOffMultiplier can't be less than 0");
        }
        mBackOffMultiplier = backOffMultiplier;
        return this;
    }

    /**
     * Set the credentials store which is used by the library to store user credentials. If no credentials store is
     * set, the net.gini.android.authorization.SharedPreferencesCredentialsStore is used by default.
     *
     * @param credentialsStore A credentials store instance (specified by the CredentialsStore interface).
     * @return The builder instance to enable chaining.
     */
    public GiniBuilder setCredentialsStore(@NonNull CredentialsStore credentialsStore) {
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
    public GiniBuilder setCache(@NonNull Cache cache) {
        mCache = cache;
        return this;
    }

    /**
     * Builds the Gini instance with the configuration settings of the builder instance.
     *
     * @return The fully configured Gini instance.
     */
    public Gini build() {
        return new Gini(getDocumentTaskManager(), getCredentialsStore());
    }

    /**
     * Helper method to create (and store) the RequestQueue which is used for both the requests to the Gini API and the
     * Gini User Center API.
     *
     * @return The RequestQueue instance.
     */
    @NonNull
    private synchronized RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            RequestQueueBuilder requestQueueBuilder = new RequestQueueBuilder(mContext);
            requestQueueBuilder.setHostnames(getHostnames());
            if (mCache != null) {
                requestQueueBuilder.setCache(mCache);
            }
            if (mNetworkSecurityConfigResId != 0) {
                requestQueueBuilder.setNetworkSecurityConfigResId(mNetworkSecurityConfigResId);
            }
            mRequestQueue = requestQueueBuilder.build();
        }
        return mRequestQueue;
    }

    private String getApiBaseUrl() {
        return mApiBaseUrl != null ? mApiBaseUrl : mGiniApiType.getBaseUrl();
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
    private synchronized ApiCommunicator getApiCommunicator() {
        if (mApiCommunicator == null) {
            mApiCommunicator = new ApiCommunicator(getApiBaseUrl(), mGiniApiType, getRequestQueue(),
                    getRetryPolicyFactory());
        }
        return mApiCommunicator;
    }

    /**
     * Helper method to create (and store) the ApiCommunicator instance which is used to do the requests to the Gini API.
     *
     * @return The ApiCommunicator instance.
     */
    @NonNull
    private synchronized Moshi getMoshi() {
        if (mMoshi == null) {
            mMoshi = new Moshi.Builder().build();
        }
        return mMoshi;
    }

    /**
     * Helper method to create (and store) the instance of the CredentialsStore implementation which is used to store
     * user credentials. If the credentials store was previously configured via the builder, the previously configured
     * instance is used. Otherwise, a net.gini.android.authorization.EncryptedCredentialsStore instance is
     * created by default.
     *
     * @return The CredentialsStore instance.
     */
    @NonNull
    private synchronized CredentialsStore getCredentialsStore() {
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
     * Helper method to create (and store) the UserCenterApiCommunicator instance which is used to do the requests to
     * the Gini User Center API.
     *
     * @return The ApiCommunicator instance.
     */
    @NonNull
    private synchronized UserCenterAPICommunicator getUserCenterAPICommunicator() {
        if (mUserCenterApiCommunicator == null) {
            mUserCenterApiCommunicator =
                    new UserCenterAPICommunicator(getRequestQueue(), mUserCenterApiBaseUrl,
                            mGiniApiType, mClientId, mClientSecret,
                            getRetryPolicyFactory());
        }
        return mUserCenterApiCommunicator;
    }

    /**
     * Helper method to create a {@link RetryPolicyFactory} instance which is used to create a
     * {@link com.android.volley.RetryPolicy} for each request.
     *
     * @return The RetryPolicyFactory instance.
     */
    @NonNull
    private synchronized RetryPolicyFactory getRetryPolicyFactory() {
        if (mRetryPolicyFactory == null) {
            mRetryPolicyFactory = new DefaultRetryPolicyFactory(mTimeoutInMs, mMaxRetries,
                    mBackOffMultiplier);
        }
        return mRetryPolicyFactory;
    }

    /**
     * Helper method to create a UserCenterManager instance which is used to manage Gini user accounts.
     *
     * @return The UserCenterManager instance.
     */
    @NonNull
    private synchronized UserCenterManager getUserCenterManager() {
        if (mUserCenterManager == null) {
            mUserCenterManager = new UserCenterManager(getUserCenterAPICommunicator());
        }
        return mUserCenterManager;
    }

    /**
     * Helper method to create a DocumentTaskManager instance.
     *
     * @return The DocumentTaskManager instance.
     */
    @NonNull
    private synchronized DocumentTaskManager getDocumentTaskManager() {
        if (mDocumentTaskManager == null) {
            mDocumentTaskManager = new DocumentTaskManager(getApiCommunicator(),
                    getSessionManager(), mGiniApiType, getMoshi());
        }
        return mDocumentTaskManager;
    }

    /**
     * Return the {@link SessionManager} set via #setSessionManager. If no SessionManager has been set, default to
     * {@link AnonymousSessionManager}.
     *
     * @return The SessionManager instance.
     */
    @NonNull
    public synchronized SessionManager getSessionManager() {
        if (mSessionManager == null) {
            mSessionManager = new AnonymousSessionManager(mEmailDomain, getUserCenterManager(),
                    getCredentialsStore());
        }
        return mSessionManager;
    }

}

