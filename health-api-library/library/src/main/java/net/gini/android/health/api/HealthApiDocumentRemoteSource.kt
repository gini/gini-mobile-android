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
) : DocumentRemoteSource(coroutineContext, documentService, giniApiType, baseUriString) {

    internal suspend fun getPages(documentId: String): List<PageResponse> =
        withContext(coroutineContext) {
            val response = SafeApiRequest.apiRequest {
                documentService.getPages(
                    headerMap(contentType = giniApiType.giniJsonMediaType), documentId
                )
            }
            response.body() ?: throw ApiException.forResponse("Empty response body", response)
        }

    internal suspend fun getPaymentProviders(): List<PaymentProviderResponse> =
        withContext(coroutineContext) {
            val response = SafeApiRequest.apiRequest {
                documentService.getPaymentProviders(
                    headerMap(contentType = giniApiType.giniJsonMediaType)
                )
            }
            response.body() ?: throw ApiException.forResponse("Empty response body", response)
        }

    internal suspend fun getPaymentProvider(providerId: String): PaymentProviderResponse =
        withContext(coroutineContext) {
            val response = SafeApiRequest.apiRequest {
                documentService.getPaymentProvider(
                    headerMap(contentType = giniApiType.giniJsonMediaType), providerId
                )
            }
            response.body() ?: throw ApiException.forResponse("Empty response body", response)
        }

    suspend fun createPaymentRequest(paymentRequestInput: PaymentRequestInput): String =
        createPaymentRequest(headerMap(contentType = giniApiType.giniJsonMediaType), paymentRequestInput)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun createPaymentRequest(
        accessToken: String,
        paymentRequestInput: PaymentRequestInput
    ): String =
        createPaymentRequest(
            bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType),
            paymentRequestInput
        )

    private suspend fun createPaymentRequest(
        headers: Map<String, String>,
        paymentRequestInput: PaymentRequestInput
    ): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.createPaymentRequest(headers, paymentRequestInput.toPaymentRequestBody())
        }

        response.headers()["location"]?.substringAfterLast("/")
            ?: throw ApiException.forResponse("Location is missing from header", response)
    }

    suspend fun getPaymentRequestDocument(paymentRequestId: String): ByteArray =
        getPaymentRequestDocument(
            headerMap(
                contentType = giniApiType.giniPaymentRequestDocumentMediaType,
                accept = giniApiType.giniPaymentRequestDocumentMediaType
            ),
            paymentRequestId
        )

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun getPaymentRequestDocument(accessToken: String, paymentRequestId: String): ByteArray =
        getPaymentRequestDocument(
            bearerHeaderMap(
                accessToken,
                contentType = giniApiType.giniPaymentRequestDocumentMediaType,
                accept = giniApiType.giniPaymentRequestDocumentMediaType
            ),
            paymentRequestId
        )

    private suspend fun getPaymentRequestDocument(
        headers: Map<String, String>,
        paymentRequestId: String
    ): ByteArray = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequestDocument(headers, paymentRequestId)
        }
        response.body()?.bytes() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun deletePaymentRequest(paymentRequestId: String): Unit =
        deletePaymentRequest(
            headerMap(contentType = giniApiType.giniPaymentRequestDocumentMediaType),
            paymentRequestId
        )

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun deletePaymentRequest(accessToken: String, paymentRequestId: String): Unit =
        deletePaymentRequest(
            bearerHeaderMap(accessToken, contentType = giniApiType.giniPaymentRequestDocumentMediaType),
            paymentRequestId
        )

    private suspend fun deletePaymentRequest(
        headers: Map<String, String>,
        paymentRequestId: String
    ): Unit = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.deletePaymentRequest(headers, paymentRequestId)
        }
        response.body()
    }

    suspend fun getPaymentRequestImage(paymentRequestId: String): ByteArray =
        getPaymentRequestImage(
            headerMap(
                contentType = giniApiType.giniPaymentRequestDocumentPngMediaType,
                accept = giniApiType.giniPaymentRequestDocumentPngMediaType
            ),
            paymentRequestId
        )

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun getPaymentRequestImage(accessToken: String, paymentRequestId: String): ByteArray =
        getPaymentRequestImage(
            bearerHeaderMap(
                accessToken,
                contentType = giniApiType.giniPaymentRequestDocumentPngMediaType,
                accept = giniApiType.giniPaymentRequestDocumentPngMediaType
            ),
            paymentRequestId
        )

    private suspend fun getPaymentRequestImage(
        headers: Map<String, String>,
        paymentRequestId: String
    ): ByteArray = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequestDocument(headers, paymentRequestId)
        }
        response.body()?.bytes() ?: throw ApiException.forResponse(
            "Empty response body",
            response
        )
    }

    suspend fun getConfigurations(): ConfigurationResponse =
        getConfigurations(headerMap(contentType = giniApiType.giniJsonMediaType))

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun getConfigurations(accessToken: String): ConfigurationResponse =
        getConfigurations(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType))

    private suspend fun getConfigurations(headers: Map<String, String>): ConfigurationResponse =
        withContext(coroutineContext) {
            val response = SafeApiRequest.apiRequest {
                documentService.getConfigurations(headers)
            }
            response.body() ?: throw ApiException.forResponse("Empty response body", response)
        }

    suspend fun deleteDocuments(documentIds: List<String>): Unit =
        deleteDocuments(headerMap(contentType = giniApiType.giniJsonMediaType), documentIds)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun deleteDocuments(accessToken: String, documentIds: List<String>): Unit =
        deleteDocuments(
            bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType),
            documentIds
        )

    private suspend fun deleteDocuments(
        headers: Map<String, String>,
        documentIds: List<String>
    ): Unit = withContext(coroutineContext) {
        val response =
            SafeApiRequest.apiRequest {
                documentService.batchDeleteDocuments(headers, documentIds)
            }
        response.body()
    }

    suspend fun deletePaymentRequests(paymentRequestIds: List<String>): Unit =
        deletePaymentRequests(headerMap(contentType = giniApiType.giniJsonMediaType), paymentRequestIds)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun deletePaymentRequests(accessToken: String, paymentRequestIds: List<String>): Unit =
        deletePaymentRequests(
            bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType),
            paymentRequestIds
        )

    private suspend fun deletePaymentRequests(
        headers: Map<String, String>,
        paymentRequestIds: List<String>
    ): Unit = withContext(coroutineContext) {
        val response =
            SafeApiRequest.apiRequest {
                documentService.batchDeletePaymentRequests(headers, paymentRequestIds)
            }
        response.body()
    }

    private companion object {
        private const val ACCESS_TOKEN_DEPRECATION_MESSAGE =
            "The Authorization header is added by the SDK's session interceptor in the OkHttp layer. " +
                    "Use the overload without an accessToken parameter."
    }
}
