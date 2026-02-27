package net.gini.android.health.api

import kotlinx.coroutines.withContext
import net.gini.android.core.api.DocumentRemoteSource
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.SafeApiRequest
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.health.api.models.toPaymentRequestBody
import net.gini.android.health.api.response.ConfigurationResponse
import net.gini.android.health.api.response.PageResponse
import net.gini.android.health.api.response.PaymentProviderResponse
import kotlin.coroutines.CoroutineContext

/**
 * Created by Alpár Szotyori on 14.10.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Internal use only.
 */
class HealthApiDocumentRemoteSource internal constructor(
    override var coroutineContext: CoroutineContext,
    private val documentService: HealthApiDocumentService,
    private val giniApiType: GiniHealthApiType,
    baseUriString: String,
): DocumentRemoteSource(coroutineContext, documentService, giniApiType, baseUriString) {

    internal suspend fun getPages(documentId: String): List<PageResponse> = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPages(buildHeaderMap(contentType = giniApiType.giniJsonMediaType), documentId)
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    internal suspend fun getPaymentProviders(): List<PaymentProviderResponse> = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentProviders(buildHeaderMap(contentType = giniApiType.giniJsonMediaType))
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    internal suspend fun getPaymentProvider(providerId: String): PaymentProviderResponse = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentProvider(buildHeaderMap(contentType = giniApiType.giniJsonMediaType), providerId)
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun createPaymentRequest(paymentRequestInput: PaymentRequestInput): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.createPaymentRequest(
                buildHeaderMap(contentType = giniApiType.giniJsonMediaType),
                paymentRequestInput.toPaymentRequestBody()
            )
        }

        response.headers()["location"]?.substringAfterLast("/")
            ?: throw ApiException.forResponse("Location is missing from header", response)
    }

    suspend fun getPaymentRequestDocument(paymentRequestId: String): ByteArray = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequestDocument(
                buildHeaderMap(contentType = giniApiType.giniPaymentRequestDocumentMediaType, accept = giniApiType.giniPaymentRequestDocumentMediaType),
                paymentRequestId
            )
        }
        response.body()?.bytes() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun deletePaymentRequest(paymentRequestId: String): Unit = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.deletePaymentRequest(
                buildHeaderMap(contentType = giniApiType.giniPaymentRequestDocumentMediaType),
                paymentRequestId
            )
        }
        response.body()
    }

    suspend fun getPaymentRequestImage(paymentRequestId: String): ByteArray = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequestDocument(
                buildHeaderMap(contentType = giniApiType.giniPaymentRequestDocumentPngMediaType, accept = giniApiType.giniPaymentRequestDocumentPngMediaType),
                paymentRequestId
            )
        }
        response.body()?.bytes() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getConfigurations(): ConfigurationResponse = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getConfigurations(
                buildHeaderMap(contentType = giniApiType.giniJsonMediaType)
            )
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun deleteDocuments(documentIds: List<String>): Unit = withContext(coroutineContext) {
        val response =
            SafeApiRequest.apiRequest {
            documentService.batchDeleteDocuments(
                buildHeaderMap(contentType = giniApiType.giniJsonMediaType),
                documentIds
            )
        }
        response.body()
    }

    suspend fun deletePaymentRequests(paymentRequestIds: List<String>): Unit = withContext(coroutineContext) {
        val response =
            SafeApiRequest.apiRequest {
            documentService.batchDeletePaymentRequests(
                buildHeaderMap(contentType = giniApiType.giniJsonMediaType),
                paymentRequestIds
            )
        }
        response.body()
    }
}