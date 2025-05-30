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

    internal suspend fun getPages(accessToken: String, documentId: String): List<PageResponse> = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPages(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), documentId)
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    internal suspend fun getPaymentProviders(accessToken: String, ): List<PaymentProviderResponse> = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentProviders(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType))
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    internal suspend fun getPaymentProvider(accessToken: String, providerId: String): PaymentProviderResponse = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentProvider(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), providerId)
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun createPaymentRequest(accessToken: String, paymentRequestInput: PaymentRequestInput): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.createPaymentRequest(
                bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType),
                paymentRequestInput.toPaymentRequestBody()
            )
        }

        response.headers()["location"]?.substringAfterLast("/")
            ?: throw ApiException.forResponse("Location is missing from header", response)
    }

    suspend fun getPaymentRequestDocument(accessToken: String, paymentRequestId: String): ByteArray = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequestDocument(
                bearerHeaderMap(accessToken, contentType = giniApiType.giniPaymentRequestDocumentMediaType, accept = giniApiType.giniPaymentRequestDocumentMediaType),
                paymentRequestId
            )
        }
        response.body()?.bytes() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun deletePaymentRequest(accessToken: String, paymentRequestId: String): Unit = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.deletePaymentRequest(
                bearerHeaderMap(accessToken, contentType = giniApiType.giniPaymentRequestDocumentMediaType),
                paymentRequestId
            )
        }
        response.body()
    }

    suspend fun getPaymentRequestImage(accessToken: String, paymentRequestId: String): ByteArray = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequestDocument(
                bearerHeaderMap(accessToken, contentType = giniApiType.giniPaymentRequestDocumentPngMediaType, accept = giniApiType.giniPaymentRequestDocumentPngMediaType),
                paymentRequestId
            )
        }
        response.body()?.bytes() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getConfigurations(accessToken: String): ConfigurationResponse = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getConfigurations(
                bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType)
            )
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun deleteDocuments(accessToken: String, documentIds: List<String>): Unit = withContext(coroutineContext) {
        val response =
            SafeApiRequest.apiRequest {
            documentService.batchDeleteDocuments(
                bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType),
                documentIds
            )
        }
        response.body()
    }

    suspend fun deletePaymentRequests(accessToken: String, paymentRequestIds: List<String>): Unit = withContext(coroutineContext) {
        val response =
            SafeApiRequest.apiRequest {
            documentService.batchDeletePaymentRequests(
                bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType),
                paymentRequestIds
            )
        }
        response.body()
    }
}