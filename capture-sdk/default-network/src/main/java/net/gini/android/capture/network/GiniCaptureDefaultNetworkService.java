package net.gini.android.capture.network;

import static net.gini.android.capture.network.logging.UtilKt.getResponseDetails;
import static net.gini.android.capture.network.logging.UtilKt.toErrorEvent;

import android.content.Context;
import android.text.TextUtils;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;

import com.android.volley.Cache;
import com.android.volley.VolleyError;

import net.gini.android.bank.api.BankApiDocumentTaskManager;
import net.gini.android.core.api.DocumentMetadata;
import net.gini.android.bank.api.GiniBankAPI;
import net.gini.android.bank.api.GiniBankAPIBuilder;
import net.gini.android.core.api.authorization.CredentialsStore;
import net.gini.android.core.api.authorization.SessionManager;
import net.gini.android.core.api.authorization.SharedPreferencesCredentialsStore;
import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.logging.ErrorLog;
import net.gini.android.capture.network.model.CompoundExtractionsMapper;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureReturnReason;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.network.model.ReturnReasonsMapper;
import net.gini.android.capture.network.model.SpecificExtractionMapper;
import net.gini.android.capture.util.CancellationToken;
import net.gini.android.capture.util.NoOpCancellationToken;
import net.gini.android.bank.api.models.ExtractionsContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.TrustManager;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Alpar Szotyori on 30.01.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Default implementation of the network related tasks required by the Gini Capture SDK.
 *
 * <p> Relies on the <a href="http://developer.gini.net/gini-pay-api-lib-android/">Gini Bank API lib</a> for
 * executing the requests, which implements communication with the Gini API using generated
 * anonymous Gini users.
 *
 * <p><b>Important:</b> Access to the Gini User Center API is required which is restricted to
 * selected clients only. Contact Gini if you require access.
 *
 * <p> To create an instance use the {@link GiniCaptureDefaultNetworkService.Builder} returned by the
 * {@link #builder(Context)} method.
 *
 * <p> In order for the Gini Capture SDK to use this implementation pass an instance of it to
 * {@link GiniCapture.Builder#setGiniCaptureNetworkService(GiniCaptureNetworkService)} when creating a
 * {@link GiniCapture} instance.
 */
public class GiniCaptureDefaultNetworkService implements GiniCaptureNetworkService {

    private static final Logger LOG = LoggerFactory.getLogger(
            GiniCaptureDefaultNetworkService.class);

    private final GiniBankAPI mGiniApi;
    private final Map<String, net.gini.android.core.api.models.Document> mGiniApiDocuments = new HashMap<>();
    private final DocumentMetadata mDocumentMetadata;
    private net.gini.android.core.api.models.Document mAnalyzedGiniApiDocument;

    /**
     * Creates a new {@link GiniCaptureDefaultNetworkService.Builder} to configure and create a new
     * instance.
     *
     * @param context Android context
     *
     * @return a new {@link GiniCaptureDefaultNetworkService.Builder}
     */
    public static Builder builder(@NonNull final Context context) {
        return new Builder(context);
    }

    GiniCaptureDefaultNetworkService(@NonNull final GiniBankAPI giniApi,
                                     @Nullable final DocumentMetadata documentMetadata) {
        mGiniApi = giniApi;
        mDocumentMetadata = documentMetadata;
    }

    @Override
    public CancellationToken upload(@NonNull final Document document,
            @NonNull final GiniCaptureNetworkCallback<Result, Error> callback) {
        LOG.debug("Upload document {}", document.getId());
        if (document.getData() == null) {
            final Error error = new Error("Document has no data. Did you forget to load it?");
            LOG.error("Document upload failed for {}: {}", document.getId(), error.getMessage());
            callback.failure(error);
            return new NoOpCancellationToken();
        }
        if (document instanceof GiniCaptureMultiPageDocument) {
            final Error error = new Error(
                    "Multi-page document cannot be uploaded. You have to upload each of its page documents separately.");
            LOG.error("Document upload failed for {}: {}", document.getId(), error.getMessage());
            callback.failure(error);
            return new NoOpCancellationToken();
        }
        final BankApiDocumentTaskManager documentTaskManager = mGiniApi.getDocumentTaskManager();
        final Task<net.gini.android.core.api.models.Document> createDocumentTask;
        if (mDocumentMetadata != null) {
            createDocumentTask = documentTaskManager.createPartialDocument(document.getData(),
                    document.getMimeType(), null, null, mDocumentMetadata);
        } else {
            createDocumentTask = documentTaskManager.createPartialDocument(document.getData(),
                    document.getMimeType(), null, null);
        }
        createDocumentTask.continueWith(new Continuation<net.gini.android.core.api.models.Document, Void>() {
                    @Override
                    public Void then(final Task<net.gini.android.core.api.models.Document> task)
                            throws Exception {
                        if (task.isFaulted()) {
                            final Error error = new Error(getTaskErrorMessage(task), task.getError());
                            LOG.error("Document upload failed for {}: {}", document.getId(),
                                    error.getMessage());
                            callback.failure(error);
                        } else if (task.getResult() != null) {
                            final net.gini.android.core.api.models.Document apiDocument = task.getResult();
                            LOG.debug("Document upload success for {}: {}", document.getId(),
                                    apiDocument);
                            mGiniApiDocuments.put(apiDocument.getId(), apiDocument);
                            callback.success(new Result(apiDocument.getId()));
                        } else {
                            LOG.debug("Document upload cancelled for {}", document.getId());
                            callback.cancelled();
                        }
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
        return new NoOpCancellationToken();
    }

    private String getTaskErrorMessage(@NonNull final Task task) {
        if (!task.isFaulted()) {
            return "";
        }
        String errorMessage = "unknown";
        if (task.getError() != null) {
            errorMessage = task.getError().getMessage();
            if (task.getError() instanceof VolleyError) {
                errorMessage = getResponseDetails((VolleyError) task.getError());
            }
        }
        return errorMessage;
    }


    @Override
    public CancellationToken delete(@NonNull final String giniApiDocumentId,
            @NonNull final GiniCaptureNetworkCallback<Result, Error> callback) {
        LOG.debug("Delete document with api id {}", giniApiDocumentId);
        mGiniApi.getDocumentTaskManager().deletePartialDocumentAndParents(giniApiDocumentId)
                .continueWith(new Continuation<String, Void>() {
                    @Override
                    public Void then(final Task<String> task) throws Exception {
                        if (task.isFaulted()) {
                            final Error error = new Error(getTaskErrorMessage(task), task.getError());
                            LOG.error("Document deletion failed for api id {}: {}",
                                    giniApiDocumentId,
                                    error.getMessage());
                            callback.failure(error);
                        } else if (task.getResult() != null) {
                            LOG.debug("Document deletion success for api id {}", giniApiDocumentId);
                            callback.success(new Result(giniApiDocumentId));
                        } else {
                            LOG.debug("Document deletion cancelled for api id {}",
                                    giniApiDocumentId);
                            callback.cancelled();
                        }
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
        return new NoOpCancellationToken();
    }

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    @Override
    public CancellationToken analyze(
            @NonNull final LinkedHashMap<String, Integer> giniApiDocumentIdRotationMap,
            @NonNull final GiniCaptureNetworkCallback<AnalysisResult, Error> callback) {
        LOG.debug("Analyze documents {}", giniApiDocumentIdRotationMap);
        final LinkedHashMap<net.gini.android.core.api.models.Document, Integer> giniApiDocumentRotationMap =
                new LinkedHashMap<>();
        final boolean success = collectGiniApiDocuments(giniApiDocumentRotationMap,
                giniApiDocumentIdRotationMap, callback);
        if (!success) {
            return new NoOpCancellationToken();
        }
        mAnalyzedGiniApiDocument = null; // NOPMD
        final AtomicBoolean isCancelled = new AtomicBoolean();
        final AtomicReference<net.gini.android.core.api.models.Document> compositeDocument =
                new AtomicReference<>();
        mGiniApi.getDocumentTaskManager().createCompositeDocument(giniApiDocumentRotationMap, null)
                .onSuccessTask(
                        new Continuation<net.gini.android.core.api.models.Document,
                                Task<net.gini.android.core.api.models.Document>>() {
                            @Override
                            public Task<net.gini.android.core.api.models.Document> then(
                                    final Task<net.gini.android.core.api.models.Document> task)
                                    throws Exception {
                                if (isCancelled.get()) {
                                    LOG.debug(
                                            "Document analysis cancelled after composite document creation for documents {}",
                                            giniApiDocumentIdRotationMap);
                                    return Task.cancelled();
                                }
                                if (task.isCancelled()) {
                                    LOG.debug(
                                            "Composite document creation cancelled for documents {}",
                                            giniApiDocumentIdRotationMap);
                                    return task;
                                }
                                final net.gini.android.core.api.models.Document giniApiDocument =
                                        task.getResult();
                                // Composite document needed to create the AnalysisResult later
                                compositeDocument.set(giniApiDocument);
                                mGiniApiDocuments.put(giniApiDocument.getId(), giniApiDocument);
                                return mGiniApi.getDocumentTaskManager().pollDocument(
                                        giniApiDocument);
                            }
                        })
                .onSuccessTask(
                        new Continuation<net.gini.android.core.api.models.Document,
                                Task<ExtractionsContainer>>() {
                            @Override
                            public Task<ExtractionsContainer> then(
                                    final Task<net.gini.android.core.api.models.Document> task)
                                    throws Exception {
                                if (isCancelled.get()) {
                                    LOG.debug(
                                            "Document analysis cancelled after polling for documents {}",
                                            giniApiDocumentIdRotationMap);
                                    return Task.cancelled();
                                }
                                final net.gini.android.core.api.models.Document giniApiDocument =
                                        task.getResult();
                                if (task.isCancelled()) {
                                    LOG.debug(
                                            "Composite document polling cancelled for documents {}",
                                            giniApiDocumentIdRotationMap);
                                    return Task.cancelled();
                                }
                                return mGiniApi.getDocumentTaskManager().getAllExtractions(
                                        giniApiDocument);
                            }
                        })
                .continueWith(
                        new Continuation<ExtractionsContainer, Void>() {
                            @Override
                            public Void then(
                                    final Task<ExtractionsContainer> task)
                                    throws Exception {
                                if (task.isFaulted()) {
                                    final Error error = new Error(getTaskErrorMessage(task), task.getError());
                                    LOG.error("Document analysis failed for documents {}: {}",
                                            giniApiDocumentIdRotationMap, error.getMessage());
                                    callback.failure(error);
                                } else if (task.getResult() != null) {
                                    mAnalyzedGiniApiDocument = compositeDocument.get();
                                    final Map<String, GiniCaptureSpecificExtraction> extractions =
                                            SpecificExtractionMapper.mapToGiniCapture(task.getResult().getSpecificExtractions());
                                    final Map<String, GiniCaptureCompoundExtraction> compoundExtractions =
                                            CompoundExtractionsMapper.mapToGiniCapture(task.getResult().getCompoundExtractions());
                                    final List<GiniCaptureReturnReason> returnReasons =
                                            ReturnReasonsMapper.mapToGiniCapture((task.getResult().getReturnReasons()));
                                    LOG.debug("Document analysis success for documents {}: extractions = {}; compoundExtractions = {}; returnReasons = {}",
                                            giniApiDocumentIdRotationMap, extractions, compoundExtractions, returnReasons);
                                    callback.success(
                                            new AnalysisResult(compositeDocument.get().getId(),
                                                    extractions, compoundExtractions, returnReasons));
                                } else {
                                    LOG.debug("Document analysis cancelled for documents {}",
                                            giniApiDocumentIdRotationMap);
                                    callback.cancelled();
                                }
                                return null;
                            }
                        }, Task.UI_THREAD_EXECUTOR);
        return new CancellationToken() {
            @Override
            public void cancel() {
                LOG.debug("Document analaysis cancellation requested for documents {}",
                        giniApiDocumentIdRotationMap);
                isCancelled.set(true);
                if (compositeDocument.get() != null) {
                    mGiniApi.getDocumentTaskManager().cancelDocumentPolling(
                            compositeDocument.get());
                }
            }
        };

    }

    @Override
    public void handleErrorLog(@NonNull ErrorLog errorLog) {
        LOG.error(errorLog.toString(), errorLog.getException());
        mGiniApi.getDocumentTaskManager().logErrorEvent(toErrorEvent(errorLog));
    }

    @Override
    public void cleanup() {
        mAnalyzedGiniApiDocument = null; // NOPMD
        mGiniApiDocuments.clear();
    }

    private boolean collectGiniApiDocuments(
            @NonNull final LinkedHashMap<net.gini.android.core.api.models.Document, Integer> // NOPMD
                    giniApiDocumentRotationMap,
            @NonNull final LinkedHashMap<String, Integer> giniApiDocumentIdRotationMap, // NOPMD
            @NonNull final GiniCaptureNetworkCallback<AnalysisResult, Error> callback) {
        for (final Map.Entry<String, Integer> entry : giniApiDocumentIdRotationMap.entrySet()) {
            final net.gini.android.core.api.models.Document document = mGiniApiDocuments.get(entry.getKey());
            if (document == null) {
                final Error error = new Error("Missing partial document."); // NOPMD
                LOG.error("Document analysis failed for documents {}: {}",
                        giniApiDocumentIdRotationMap,
                        error.getMessage());
                callback.failure(error);
                return false;
            }
            giniApiDocumentRotationMap.put(document, entry.getValue());
        }
        return true;
    }

    @Nullable
    net.gini.android.core.api.models.Document getAnalyzedGiniApiDocument() {
        return mAnalyzedGiniApiDocument;
    }

    GiniBankAPI getGiniApi() {
        return mGiniApi;
    }

    /**
     * Builder for configuring a new instance of the {@link GiniCaptureDefaultNetworkService}.
     */
    public static class Builder {

        private final Context mContext;
        private String mClientId;
        private String mClientSecret;
        private String mEmailDomain;
        private SessionManager mSessionManager;
        private String mBaseUrl;
        private String mUserCenterBaseUrl;
        private Cache mCache;
        private CredentialsStore mCredentialsStore;
        @XmlRes
        private int mNetworkSecurityConfigResId;
        private long mConnectionTimeout;
        private TimeUnit mConnectionTimeoutUnit;
        private int mMaxNumberOfRetries;
        private float mBackoffMultiplier;
        private DocumentMetadata mDocumentMetadata;
        private TrustManager mTrustManager;

        Builder(@NonNull final Context context) {
            mContext = context;
        }

        /**
         * Create a new instance of the {@link GiniCaptureDefaultNetworkService}.
         *
         * @return new {@link GiniCaptureDefaultNetworkService} instance
         */
        @NonNull
        public GiniCaptureDefaultNetworkService build() {
            final GiniBankAPIBuilder giniApiBuilder;
            if (mSessionManager != null) {
                giniApiBuilder = new GiniBankAPIBuilder(mContext, mSessionManager);
            } else {
                giniApiBuilder = new GiniBankAPIBuilder(mContext, mClientId, mClientSecret, mEmailDomain);
            }
            if (!TextUtils.isEmpty(mBaseUrl)) {
                giniApiBuilder.setApiBaseUrl(mBaseUrl);
            }
            if (!TextUtils.isEmpty(mUserCenterBaseUrl)) {
                giniApiBuilder.setUserCenterApiBaseUrl(mUserCenterBaseUrl);
            }
            if (mCache != null) {
                giniApiBuilder.setCache(mCache);
            }
            if (mCredentialsStore != null) {
                giniApiBuilder.setCredentialsStore(mCredentialsStore);
            }
            if (mNetworkSecurityConfigResId != 0) {
                giniApiBuilder.setNetworkSecurityConfigResId(mNetworkSecurityConfigResId);
            }
            if (mConnectionTimeoutUnit != null) {
                giniApiBuilder.setConnectionTimeoutInMs(
                        (int) TimeUnit.MILLISECONDS.convert(mConnectionTimeout,
                                mConnectionTimeoutUnit));
            }
            if (mMaxNumberOfRetries >= 0) {
                giniApiBuilder.setMaxNumberOfRetries(mMaxNumberOfRetries);
            }
            if (mBackoffMultiplier >= 0) {
                giniApiBuilder.setConnectionBackOffMultiplier(mBackoffMultiplier);
            }
            if (mTrustManager != null) {
                giniApiBuilder.setTrustManager(mTrustManager);
            }
            final GiniBankAPI giniApi = giniApiBuilder.build();
            return new GiniCaptureDefaultNetworkService(giniApi, mDocumentMetadata);
        }

        /**
         * Set your Gini API client ID and secret. The email domain is used when generating
         * anonymous Gini users in the form of {@code UUID@your-email-domain}.
         *
         * @param clientId     your application's client ID for the Gini API
         * @param clientSecret your application's client secret for the Gini API
         * @param emailDomain  the email domain which is used for created Gini users
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setClientCredentials(@NonNull final String clientId,
                @NonNull final String clientSecret, @NonNull final String emailDomain) {
            mClientId = clientId;
            mClientSecret = clientSecret;
            mEmailDomain = emailDomain;
            return this;
        }

        /**
         * Set a custom {@link SessionManager} implementation for handling sessions.
         *
         * @param sessionManager the {@link SessionManager} to use
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setSessionManager(@NonNull final SessionManager sessionManager) {
            mSessionManager = sessionManager;
            return this;
        }

        /**
         * Set the base URL of the Gini API.
         *
         * @param baseUrl custom Gini API base URL
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setBaseUrl(@NonNull final String baseUrl) {
            mBaseUrl = baseUrl;
            return this;
        }

        /**
         * Set the base URL of the Gini User Center API.
         *
         * @param userCenterBaseUrl custom Gini API base URL
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setUserCenterBaseUrl(@NonNull final String userCenterBaseUrl) {
            mUserCenterBaseUrl = userCenterBaseUrl;
            return this;
        }

        /**
         * Set the cache implementation to use with Volley. If no cache is set, the default Volley
         * cache will be used.
         *
         * @param cache a cache instance (specified by the com.android.volley.Cache interface)
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setCache(@NonNull final Cache cache) {
            mCache = cache;
            return this;
        }

        /**
         * Set the credentials store which is used by the Gini Bank API lib to store user credentials. If
         * no credentials store is set, the {@link SharedPreferencesCredentialsStore} from the Gini
         * API SDK is used by default.
         *
         * @param credentialsStore a credentials store instance (specified by the CredentialsStore
         *                         interface)
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setCredentialsStore(@NonNull final CredentialsStore credentialsStore) {
            mCredentialsStore = credentialsStore;
            return this;
        }

        /**
         * Set the resource id for the network security configuration xml to enable public key pinning.
         *
         * @param networkSecurityConfigResId xml resource id
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setNetworkSecurityConfigResId(@XmlRes final int networkSecurityConfigResId) {
            mNetworkSecurityConfigResId = networkSecurityConfigResId;
            return this;
        }

        /**
         * Set the (initial) timeout for each request. A timeout error will occur if nothing is
         * received from the underlying socket in the given time span. The initial timeout will be
         * altered depending on the backoff multiplier and failed retries.
         *
         * @param connectionTimeout initial timeout
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setConnectionTimeout(final long connectionTimeout) {
            mConnectionTimeout = connectionTimeout;
            return this;
        }

        /**
         * Set the connection timeout's time unit.
         *
         * @param connectionTimeoutUnit the time unit
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setConnectionTimeoutUnit(@NonNull final TimeUnit connectionTimeoutUnit) {
            mConnectionTimeoutUnit = connectionTimeoutUnit;
            return this;
        }

        /**
         * Set the maximal number of retries for each network request.
         *
         * @param maxNumberOfRetries maximal number of retries
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setMaxNumberOfRetries(final int maxNumberOfRetries) {
            mMaxNumberOfRetries = maxNumberOfRetries;
            return this;
        }

        /**
         * Sets the backoff multiplication factor for connection retries. In case of failed retries
         * the timeout of the last request attempt is multiplied by this factor.
         *
         * @param backoffMultiplier the backoff multiplication factor
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setBackoffMultiplier(final float backoffMultiplier) {
            mBackoffMultiplier = backoffMultiplier;
            return this;
        }

        /**
         * Set additional information related to the documents. This metadata will be passed to all
         * document uploads.
         *
         * @param documentMetadata a {@link DocumentMetadata} instance containing additional
         *                         information for the uploaded documents
         * @return the {@link Builder} instance
         */
        public Builder setDocumentMetadata(@NonNull final DocumentMetadata documentMetadata) {
            mDocumentMetadata = documentMetadata;
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
         * @return the {@link Builder} instance
         */
        public Builder setTrustManager(@NonNull final TrustManager trustManager) {
            mTrustManager = trustManager;
            return this;
        }
    }

}
