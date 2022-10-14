package net.gini.android.health.api

import kotlinx.coroutines.withContext
import net.gini.android.core.api.DocumentRemoteSource
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.KSessionManager
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.SafeApiRequest
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.health.api.models.toPaymentRequestBody
import net.gini.android.health.api.response.PageResponse
import net.gini.android.health.api.response.PaymentProviderResponse
import okhttp3.RequestBody
import kotlin.coroutines.CoroutineContext

/**
 * Created by Alpár Szotyori on 14.10.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */
class HealthApiDocumentRemoteSource internal constructor(
    override var coroutineContext: CoroutineContext,
    private val documentService: HealthApiDocumentService,
    private val giniApiType: GiniHealthApiType,
    baseUriString: String
): DocumentRemoteSource(coroutineContext, documentService, giniApiType, baseUriString) {

    suspend fun sendFeedback(sessionToken: SessionToken, documentId: String, requestBody: RequestBody): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.sendFeedback(bearerHeaderMap(sessionToken, contentType = giniApiType.giniJsonMediaType), documentId, requestBody)
        }
    }

    internal suspend fun getPages(sessionToken: SessionToken, documentId: String): List<PageResponse> = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPages(bearerHeaderMap(sessionToken, contentType = giniApiType.giniJsonMediaType), documentId)
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    internal suspend fun getPaymentProviders(sessionToken: SessionToken, ): List<PaymentProviderResponse> = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentProviders(bearerHeaderMap(sessionToken, contentType = giniApiType.giniJsonMediaType))
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    internal suspend fun getPaymentProvider(sessionToken: SessionToken, providerId: String): PaymentProviderResponse = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentProvider(bearerHeaderMap(sessionToken, contentType = giniApiType.giniJsonMediaType), providerId)
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun createPaymentRequest(sessionToken: SessionToken, paymentRequestInput: PaymentRequestInput): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.createPaymentRequest(
                bearerHeaderMap(sessionToken, contentType = giniApiType.giniJsonMediaType),
                paymentRequestInput.toPaymentRequestBody()
            )
        }

        response.headers()["location"]?.substringAfterLast("/")
            ?: throw ApiException.forResponse("Location is missing from header", response)
    }
}