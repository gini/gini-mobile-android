package net.gini.android.capture.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.logging.ErrorLog
import net.gini.android.capture.network.logging.formattedErrorMessage
import net.gini.android.capture.network.model.CompoundExtractionsMapper
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.network.model.SpecificExtractionMapper
import net.gini.android.core.api.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

/**
 * Created by Alpár Szotyori on 23.10.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Default implementation of network calls which can be performed manually from outside the Gini
 * Capture SDK (e.g. for sending feedback).
 *
 * To create an instance use the [GiniCaptureDefaultNetworkApi.Builder] returned by the
 * [builder] method.
 *
 * In order to easily access this implementation pass an instance of it to
 * [GiniCapture.Builder.setGiniCaptureNetworkApi] when creating a
 * [GiniCapture] instance. You can then get the instance in your app with
 * [GiniCapture.getGiniCaptureNetworkApi].
 */
class GiniCaptureDefaultNetworkApi(
    private val defaultNetworkService: GiniCaptureDefaultNetworkService,
    coroutineContext: CoroutineContext = Dispatchers.Main
) : GiniCaptureNetworkApi {

    private val coroutineScope = CoroutineScope(coroutineContext)
    private var updatedCompoundExtractions = emptyMap<String, GiniCaptureCompoundExtraction>()

    override fun sendFeedback(
        extractions: MutableMap<String, GiniCaptureSpecificExtraction>,
        callback: GiniCaptureNetworkCallback<Void, Error>
    ) {
        coroutineScope.launch {
            val documentManager = defaultNetworkService.giniBankApi.documentManager
            val document = defaultNetworkService.analyzedGiniApiDocument
            // We require the Gini Bank API lib's net.gini.android.core.api.models.Document for sending the feedback
            if (document != null) {
                val feedbackResource = if (updatedCompoundExtractions.isEmpty()) {
                    documentManager.sendFeedbackForExtractions(document, SpecificExtractionMapper.mapToApiSdk(extractions))
                } else {
                    documentManager.sendFeedbackForExtractions(
                        document,
                        SpecificExtractionMapper.mapToApiSdk(extractions),
                        CompoundExtractionsMapper.mapToApiSdk(updatedCompoundExtractions)
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
                        LOG.error("Send feedback failed for api document {}: {}", document.id, error.message)
                        defaultNetworkService.handleErrorLog(
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
                defaultNetworkService.handleErrorLog(
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
        defaultNetworkService.giniBankApi.credentialsStore.deleteUserCredentials()
    }

    override fun setUpdatedCompoundExtractions(compoundExtractions: Map<String, GiniCaptureCompoundExtraction>) {
        updatedCompoundExtractions = compoundExtractions
    }

    /**
     * Builder for configuring a new instance of the [GiniCaptureDefaultNetworkApi].
     */
    class Builder internal constructor() {
        private var defaultNetworkService: GiniCaptureDefaultNetworkService? = null

        /**
         * Set the same [GiniCaptureDefaultNetworkService] instance you use for [GiniCapture].
         *
         * @param networkService [GiniCaptureDefaultNetworkService] instance
         * @return the [Builder] instance
         */
        fun withGiniCaptureDefaultNetworkService(
            networkService: GiniCaptureDefaultNetworkService
        ): Builder {
            defaultNetworkService = networkService
            return this
        }

        /**
         * Create a new instance of the [GiniCaptureDefaultNetworkApi].
         *
         * @return new [GiniCaptureDefaultNetworkApi] instance
         */
        fun build(): GiniCaptureDefaultNetworkApi = defaultNetworkService?.let { networkService ->
            GiniCaptureDefaultNetworkApi(networkService)
        }
            ?: throw IllegalStateException("GiniCaptureDefaultNetworkApi requires a GiniCaptureDefaultNetworkService instance.")
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GiniCaptureDefaultNetworkApi::class.java)

        /**
         * Creates a new [GiniCaptureDefaultNetworkApi.Builder] to configure and create a new instance.
         *
         * @return a new [GiniCaptureDefaultNetworkApi.Builder]
         */
        @JvmStatic
        fun builder() = Builder()
    }
}