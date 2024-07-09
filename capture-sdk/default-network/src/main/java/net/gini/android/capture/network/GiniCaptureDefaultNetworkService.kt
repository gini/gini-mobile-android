package net.gini.android.capture.network

import android.content.Context
import android.text.TextUtils
import androidx.annotation.XmlRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import net.gini.android.bank.api.GiniBankAPI
import net.gini.android.bank.api.GiniBankAPIBuilder
import net.gini.android.bank.api.models.*
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.document.GiniCaptureMultiPageDocument
import net.gini.android.capture.internal.network.AmplitudeRoot
import net.gini.android.capture.internal.network.Configuration
import net.gini.android.capture.logging.ErrorLog
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService.Companion.builder
import net.gini.android.capture.network.logging.formattedErrorMessage
import net.gini.android.capture.network.logging.toErrorEvent
import net.gini.android.capture.network.model.*
import net.gini.android.capture.util.CancellationToken
import net.gini.android.core.api.DocumentMetadata
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.authorization.SessionManager
import okhttp3.Cache
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManager
import kotlin.coroutines.CoroutineContext
import net.gini.android.bank.api.models.Configuration as BankConfiguration

/**
 * Created by Alpár Szotyori on 30.09.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Default implementation of the network related tasks required by the Gini Capture SDK.
 *
 * Relies on the [Gini Bank API Library](https://developer.gini.net/gini-mobile-android/bank-api-library/library/html/) for
 * executing the requests, which implements communication with the Gini Bank API using generated
 * anonymous Gini users.
 *
 * *Important:* Access to the Gini User Center API is required which is restricted to
 * selected clients only. Contact Gini if you require access.
 *
 * To create an instance use the [GiniCaptureDefaultNetworkService.Builder] returned by the
 * [builder] method.
 *
 * In order for the Gini Capture SDK to use this implementation pass an instance of it to
 * [GiniCapture.Builder.setGiniCaptureNetworkService] when creating a
 * [GiniCapture] instance.
 */
class GiniCaptureDefaultNetworkService(
    internal val giniBankApi: GiniBankAPI,
    private val documentMetadata: DocumentMetadata?,
    coroutineContext: CoroutineContext = Dispatchers.Main
) : GiniCaptureNetworkService {

    private val coroutineScope = CoroutineScope(coroutineContext)
    private val giniApiDocuments: MutableMap<String, net.gini.android.core.api.models.Document> =
        mutableMapOf()

    /**
     * Contains the document which was created when the user uploaded an image or a pdf for
     * analysis.
     *
     * It is `null` when extractions were retrieved without using the Gini Bank API.
     * For example when the extractions came from an EPS QR code.
     *
     * You should call this method only after the Gini Capture SDK returned the extraction results and before
     * you call [GiniCaptureDefaultNetworkService.cleanup] or [GiniCapture.cleanup].
     *
     * @return the last analyzed Gini Bank API [net.gini.android.core.api.models.Document]
     */
    var analyzedGiniApiDocument: net.gini.android.core.api.models.Document? = null
        private set


    override fun sendEvents(
        amplitudeRoot: AmplitudeRoot,
        callback: GiniCaptureNetworkCallback<Void, Error>
    ): CancellationToken? =
        launchCancellable {
            when (val configurationResource =
                giniBankApi.documentManager.sendEvents(
                    mapAmplitudeRootModelToAmplitudeRoot(
                        amplitudeRoot
                    )
                )) {
                is Resource.Success -> {
                    LOG.debug(
                        "Send events success"
                    )
                    callback.success(null)
                }

                is Resource.Error -> {
                    LOG.debug(
                        "Send events failed"
                    )
                    val error = Error(configurationResource.formattedErrorMessage)
                    LOG.error(
                        "Send events failed for {}",
                        error.message
                    )
                    callback.failure(error)
                }

                is Resource.Cancelled -> {
                    LOG.debug("Send events cancelled")
                    callback.cancelled()
                }
            }
        }

    private fun mapAmplitudeRootModelToAmplitudeRoot(amplitudeRoot: AmplitudeRoot) =
        AmplitudeRoot(
            apiKey = amplitudeRoot.apiKey,
            events = amplitudeRoot.events.map { event ->
                AmplitudeEvent(
                    userId = event.userId,
                    deviceId = event.deviceId,
                    eventType = event.eventType,
                    time = event.time,
                    platform = event.platform,
                    osVersion = event.osVersion,
                    deviceManufacturer = event.deviceManufacturer,
                    deviceBrand = event.deviceBrand,
                    deviceModel = event.deviceModel,
                    versionName = event.versionName,
                    osName = event.osName,
                    carrier = event.carrier,
                    language = event.language,
                    appSetId = event.appSetId,
                    eventProperties = event.eventProperties,
                    userProperties = event.userProperties,
                    appVersion = event.appVersion
                )
            }
        )


    override fun getConfiguration(callback: GiniCaptureNetworkCallback<Configuration, Error>): CancellationToken? =
        launchCancellable {
            when (val configurationResource = giniBankApi.documentManager.getConfigurations()) {
                is Resource.Success -> {
                    LOG.debug(
                        "Get configuration success"
                    )
                    callback.success(mapBankConfigurationToConfiguration(configurationResource.data))
                }

                is Resource.Error -> {
                    LOG.debug(
                        "Get configuration error for {}: {}"
                    )
                    val error = Error(configurationResource.formattedErrorMessage)
                    LOG.error(
                        "Document deletion failed for api id {}",
                        error.message
                    )
                    callback.failure(error)
                }

                is Resource.Cancelled -> {
                    LOG.debug("Get configuration cancelled for")
                    callback.cancelled()
                }
            }
        }

    private fun mapBankConfigurationToConfiguration(configuration: BankConfiguration) =
        Configuration(
            UUID.randomUUID(),
            configuration.clientID,
            configuration.isUserJourneyAnalyticsEnabled,
            configuration.isSkontoEnabled,
            configuration.isReturnAssistantEnabled,
            configuration.mixpanelToken ?: "",
            configuration.amplitudeApiKey ?: "",
        )

    override fun upload(
        document: Document,
        callback: GiniCaptureNetworkCallback<Result, Error>
    ): CancellationToken = launchCancellable {
        LOG.debug("Upload document {}", document.id)
        val documentData = document.data
        if (documentData == null) {
            val error = Error("Document has no data. Did you forget to load it?")
            LOG.error("Document upload failed for {}: {}", document.id, error.message)
            callback.failure(error)
            return@launchCancellable
        }
        if (document is GiniCaptureMultiPageDocument<*, *>) {
            val error = Error(
                "Multi-page document cannot be uploaded. You have to upload each of its page documents separately."
            )
            LOG.error(
                "Document upload failed for {}: {}",
                document.getId(),
                error.message
            )
            callback.failure(error)
            return@launchCancellable
        }

        val partialDocumentResource = giniBankApi.documentManager.createPartialDocument(
            document = documentData,
            contentType = document.mimeType,
            filename = null,
            documentType = null,
            documentMetadata
        )

        when (partialDocumentResource) {
            is Resource.Success -> {
                val apiDocument = partialDocumentResource.data
                LOG.debug(
                    "Document upload success for {}: {}", document.id,
                    apiDocument
                )
                giniApiDocuments[apiDocument.id] = apiDocument
                callback.success(Result(apiDocument.id))
            }

            is Resource.Error -> {

                val error = Error(
                    partialDocumentResource.responseStatusCode,
                    partialDocumentResource.responseHeaders, partialDocumentResource.exception
                )
                LOG.error(
                    "Document upload failed for {}: {}", document.id,
                    error.message
                )
                callback.failure(error)
            }

            is Resource.Cancelled -> {
                LOG.debug(
                    "Document upload cancelled for {}",
                    document.id
                )
                callback.cancelled()
            }
        }
    }

    private inline fun launchCancellable(crossinline block: suspend () -> Unit): CancellationToken {
        val job = coroutineScope.launch {
            block()
        }
        return CancellationToken { job.cancel() }
    }

    override fun delete(
        giniApiDocumentId: String,
        callback: GiniCaptureNetworkCallback<Result, Error>
    ): CancellationToken = launchCancellable {
        LOG.debug("Delete document with api id {}", giniApiDocumentId)
        val deleteResource =
            giniBankApi.documentManager.deletePartialDocumentAndParents(giniApiDocumentId)
        when (deleteResource) {
            is Resource.Success -> {
                LOG.debug("Document deletion success for api id {}", giniApiDocumentId)
                callback.success(Result(giniApiDocumentId))
            }

            is Resource.Error -> {
                val error = Error(deleteResource.formattedErrorMessage)
                LOG.error(
                    "Document deletion failed for api id {}: {}", giniApiDocumentId,
                    error.message
                )
                callback.failure(error)
            }

            is Resource.Cancelled -> {
                LOG.debug("Document deletion cancelled for api id {}", giniApiDocumentId)
                callback.cancelled()
            }
        }
    }

    override fun analyze(
        giniApiDocumentIdRotationMap: LinkedHashMap<String, Int>,
        callback: GiniCaptureNetworkCallback<AnalysisResult, Error>
    ): CancellationToken = launchCancellable {
        LOG.debug("Analyze documents {}", giniApiDocumentIdRotationMap)
        val giniApiDocumentRotationMap = giniApiDocumentIdRotationMap.mapNotNull { entry ->
            giniApiDocuments[entry.key]?.let { it to entry.value }
        }.toMap(LinkedHashMap())

        if (giniApiDocumentRotationMap.isEmpty()) {
            val error = Error("Missing partial document.")
            LOG.error(
                "Document analysis failed for documents {}: {}",
                giniApiDocumentIdRotationMap,
                error.message
            )
            callback.failure(error)
            return@launchCancellable
        }

        analyzedGiniApiDocument = null

        val compositeDocumentAndExtractionsResource =
            giniBankApi.documentManager.createCompositeDocument(giniApiDocumentRotationMap)
                .mapSuccess { compositeDocumentResource ->
                    val compositeDocument = compositeDocumentResource.data
                    giniApiDocuments[compositeDocument.id] = compositeDocument
                    giniBankApi.documentManager.getAllExtractionsWithPolling(compositeDocument)
                        .mapSuccess {
                            Resource.Success(compositeDocument to it.data)
                        }
                }
        when (compositeDocumentAndExtractionsResource) {
            is Resource.Cancelled -> {
                LOG.debug(
                    "Document analysis cancelled for documents {}",
                    giniApiDocumentIdRotationMap
                )
            }

            is Resource.Error -> {
                val error = Error(compositeDocumentAndExtractionsResource.formattedErrorMessage)
                LOG.error(
                    "Document analysis failed for documents {}: {}",
                    giniApiDocumentIdRotationMap, error.message
                )
                callback.failure(error)
            }

            is Resource.Success -> {
                val compositeDocument = compositeDocumentAndExtractionsResource.data.first
                val allExtractions = compositeDocumentAndExtractionsResource.data.second

                analyzedGiniApiDocument = compositeDocument

                val extractions =
                    SpecificExtractionMapper.mapToGiniCapture(allExtractions.specificExtractions)
                val compoundExtractions =
                    CompoundExtractionsMapper.mapToGiniCapture(allExtractions.compoundExtractions)
                val returnReasons =
                    ReturnReasonsMapper.mapToGiniCapture(allExtractions.returnReasons)

                LOG.debug(
                    "Document analysis success for documents {}: extractions = {}; compoundExtractions = {}; returnReasons = {}",
                    giniApiDocumentIdRotationMap, extractions, compoundExtractions, returnReasons
                )

                callback.success(
                    AnalysisResult(
                        compositeDocument.id,
                        extractions,
                        compoundExtractions,
                        returnReasons
                    )
                )
            }
        }
    }

    override fun sendFeedback(
        extractions: MutableMap<String, GiniCaptureSpecificExtraction>,
        compoundExtractions: MutableMap<String, GiniCaptureCompoundExtraction>,
        callback: GiniCaptureNetworkCallback<Void, Error>
    ) {
        coroutineScope.launch {
            val documentManager = giniBankApi.documentManager
            val document = analyzedGiniApiDocument
            // We require the Gini Bank API lib's net.gini.android.core.api.models.Document for sending the feedback
            if (document != null) {
                val feedbackResource = if (compoundExtractions.isEmpty()) {
                    documentManager.sendFeedbackForExtractions(
                        document,
                        SpecificExtractionMapper.mapToApiSdk(extractions)
                    )
                } else {
                    documentManager.sendFeedbackForExtractions(
                        document,
                        SpecificExtractionMapper.mapToApiSdk(extractions),
                        CompoundExtractionsMapper.mapToApiSdk(compoundExtractions)
                    )
                }
                when (feedbackResource) {
                    is Resource.Success -> {
                        LOG.debug(
                            "Send feedback success for api document {}",
                            document.id
                        )
                        callback.success(null)
                    }

                    is Resource.Error -> {
                        val error = Error(feedbackResource.formattedErrorMessage)
                        LOG.error(
                            "Send feedback failed for api document {}: {}",
                            document.id,
                            error.message
                        )
                        handleErrorLog(
                            ErrorLog(
                                description = "Failed to send feedback for document ${document.id}",
                                exception = feedbackResource.exception
                            )
                        )
                        callback.failure(error)
                    }

                    is Resource.Cancelled -> {
                        LOG.debug(
                            "Send feedback cancelled for api document {}",
                            document.id
                        )
                        callback.cancelled()
                    }
                }
            } else {
                LOG.error("Send feedback failed: no api document available")
                handleErrorLog(
                    ErrorLog(
                        description = "Failed to send feedback: no api document available",
                        exception = null
                    )
                )
                callback.failure(Error("Feedback not set: no api document available"))
            }
        }
    }

    override fun deleteGiniUserCredentials() {
        giniBankApi.credentialsStore.deleteUserCredentials()

    }

    override fun handleErrorLog(errorLog: ErrorLog) {
        coroutineScope.launch {
            LOG.error(errorLog.toString(), errorLog.exception)
            giniBankApi.documentManager.logErrorEvent(errorLog.toErrorEvent())
        }
    }

    override fun cleanup() {
        try {
            analyzedGiniApiDocument = null
            giniApiDocuments.clear()
            coroutineScope.coroutineContext.cancelChildren()
        } catch (ignored: IllegalStateException) {
        }
    }

    /**
     * Builder for configuring a new instance of the [GiniCaptureDefaultNetworkService].
     */
    class Builder internal constructor(private val mContext: Context) {
        private var clientId: String = ""
        private var clientSecret: String = ""
        private var emailDomain: String = ""
        private var sessionManager: SessionManager? = null
        private var baseUrl: String = ""
        private var userCenterBaseUrl: String = ""
        private var cache: Cache? = null
        private var credentialsStore: CredentialsStore? = null

        @XmlRes
        private var networkSecurityConfigResId = 0
        private var connectionTimeout: Long = 0
        private var connectionTimeoutUnit: TimeUnit? = null
        private var documentMetadata: DocumentMetadata? = null
        private var trustManager: TrustManager? = null
        private var isDebuggingEnabled = false

        /**
         * Create a new instance of the [GiniCaptureDefaultNetworkService].
         *
         * @return new [GiniCaptureDefaultNetworkService] instance
         */
        fun build(): GiniCaptureDefaultNetworkService {
            val giniApiBuilder = if (sessionManager != null) {
                GiniBankAPIBuilder(mContext, sessionManager = sessionManager)
            } else {
                GiniBankAPIBuilder(mContext, clientId, clientSecret, emailDomain)
            }
            if (!TextUtils.isEmpty(baseUrl)) {
                giniApiBuilder.setApiBaseUrl(baseUrl)
            }
            if (!TextUtils.isEmpty(userCenterBaseUrl)) {
                giniApiBuilder.setUserCenterApiBaseUrl(userCenterBaseUrl)
            }
            cache?.let { giniApiBuilder.setCache(it) }
            credentialsStore?.let { giniApiBuilder.setCredentialsStore(it) }
            if (networkSecurityConfigResId != 0) {
                giniApiBuilder.setNetworkSecurityConfigResId(networkSecurityConfigResId)
            }
            connectionTimeoutUnit?.let { timeoutUnit ->
                giniApiBuilder.setConnectionTimeoutInMs(
                    TimeUnit.MILLISECONDS.convert(
                        connectionTimeout,
                        timeoutUnit
                    ).toInt()
                )
            }
            trustManager?.let { giniApiBuilder.setTrustManager(it) }
            giniApiBuilder.setDebuggingEnabled(isDebuggingEnabled)
            val giniBankApi = giniApiBuilder.build()
            return GiniCaptureDefaultNetworkService(giniBankApi, documentMetadata)
        }

        /**
         * Set your Gini API client ID and secret. The email domain is used when generating
         * anonymous Gini users in the form of `UUID@your-email-domain`.
         *
         * @param clientId     your application's client ID for the Gini API
         * @param clientSecret your application's client secret for the Gini API
         * @param emailDomain  the email domain which is used for created Gini users
         *
         * @return the [Builder] instance
         */
        fun setClientCredentials(
            clientId: String,
            clientSecret: String, emailDomain: String
        ): Builder {
            this.clientId = clientId
            this.clientSecret = clientSecret
            this.emailDomain = emailDomain
            return this
        }

        /**
         * Set a custom [SessionManager] implementation for handling sessions.
         *
         * @param sessionManager the [SessionManager] to use
         *
         * @return the [Builder] instance
         */
        fun setSessionManager(sessionManager: SessionManager): Builder {
            this.sessionManager = sessionManager
            return this
        }

        /**
         * Set the base URL of the Gini API.
         *
         * @param baseUrl custom Gini API base URL
         *
         * @return the [Builder] instance
         */
        fun setBaseUrl(baseUrl: String): Builder {
            this.baseUrl = baseUrl
            return this
        }

        /**
         * Set the base URL of the Gini User Center API.
         *
         * @param userCenterBaseUrl custom Gini API base URL
         *
         * @return the [Builder] instance
         */
        fun setUserCenterBaseUrl(userCenterBaseUrl: String): Builder {
            this.userCenterBaseUrl = userCenterBaseUrl
            return this
        }

        /**
         * Set the cache implementation to use with Volley. If no cache is set, the default Volley
         * cache will be used.
         *
         * @param cache a cache instance (specified by the com.android.volley.Cache interface)
         *
         * @return the [Builder] instance
         */
        fun setCache(cache: Cache): Builder {
            this.cache = cache
            return this
        }

        /**
         * Set the credentials store which is used by the Gini Bank API lib to store user credentials. If
         * no credentials store is set, the [SharedPreferencesCredentialsStore] from the Gini
         * API SDK is used by default.
         *
         * @param credentialsStore a credentials store instance (specified by the CredentialsStore
         * interface)
         *
         * @return the [Builder] instance
         */
        fun setCredentialsStore(credentialsStore: CredentialsStore): Builder {
            this.credentialsStore = credentialsStore
            return this
        }

        /**
         * Set the resource id for the network security configuration xml to enable public key pinning.
         *
         * @param networkSecurityConfigResId xml resource id
         * @return the [Builder] instance
         */
        fun setNetworkSecurityConfigResId(@XmlRes networkSecurityConfigResId: Int): Builder {
            this.networkSecurityConfigResId = networkSecurityConfigResId
            return this
        }

        /**
         * Set the (initial) timeout for each request. A timeout error will occur if nothing is
         * received from the underlying socket in the given time span. The initial timeout will be
         * altered depending on the backoff multiplier and failed retries.
         *
         * @param connectionTimeout initial timeout
         *
         * @return the [Builder] instance
         */
        fun setConnectionTimeout(connectionTimeout: Long): Builder {
            this.connectionTimeout = connectionTimeout
            return this
        }

        /**
         * Set the connection timeout's time unit.
         *
         * @param connectionTimeoutUnit the time unit
         *
         * @return the [Builder] instance
         */
        fun setConnectionTimeoutUnit(connectionTimeoutUnit: TimeUnit): Builder {
            this.connectionTimeoutUnit = connectionTimeoutUnit
            return this
        }

        /**
         * Set additional information related to the documents. This metadata will be passed to all
         * document uploads.
         *
         * @param documentMetadata a [DocumentMetadata] instance containing additional
         * information for the uploaded documents
         * @return the [Builder] instance
         */
        fun setDocumentMetadata(documentMetadata: DocumentMetadata): Builder {
            this.documentMetadata = documentMetadata
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
         * @return the [Builder] instance
         */
        fun setTrustManager(trustManager: TrustManager): Builder {
            this.trustManager = trustManager
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
         * @return the [Builder] instance
         */
        fun setDebuggingEnabled(enabled: Boolean): Builder {
            isDebuggingEnabled = enabled
            if (isDebuggingEnabled) {
                LOG.warn("Debugging enabled. Make sure to disable debugging for release builds!")
            }
            return this
        }
    }

    companion object {
        private val LOG: Logger =
            LoggerFactory.getLogger(GiniCaptureDefaultNetworkService::class.java)

        /**
         * Creates a new [GiniCaptureDefaultNetworkService.Builder] to configure and create a new
         * instance.
         *
         * @param context Android context
         *
         * @return a new [GiniCaptureDefaultNetworkService.Builder]
         */
        @JvmStatic
        fun builder(context: Context) = Builder(context)
    }
}