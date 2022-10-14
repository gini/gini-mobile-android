package net.gini.android.bank.api

import android.net.Uri
import kotlinx.coroutines.withContext
import net.gini.android.bank.api.models.FeedbackRequestModel
import net.gini.android.bank.api.models.Payment
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.core.api.DocumentRemoteSource
import net.gini.android.core.api.DocumentService
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.KSessionManager
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.SafeApiRequest
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.coroutines.CoroutineContext

class BankApiDocumentRemoteSource(
    override var coroutineContext: CoroutineContext,
    val documentService: BankApiDocumentService,
    private val giniApiType: GiniBankApiType,
    private val sessionManager: KSessionManager,
    baseUriString: String
): DocumentRemoteSource(coroutineContext, documentService, giniApiType, sessionManager, baseUriString) {

    suspend fun sendFeedback(documentId: String, requestBody: RequestBody): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw apiResult.toApiException()
            }

            documentService.sendFeedback(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType), documentId, requestBody)
        }
    }

    suspend fun resolvePaymentRequests(id: String, input: ResolvePaymentInput): ResolvedPayment = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw apiResult.toApiException()
            }

            documentService.resolvePaymentRequests(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType), id, input)
        }
        response.body() ?: throw ApiException("Empty response body", response)
    }

    suspend fun getPayment(id: String): Payment = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw apiResult.toApiException()
            }

            documentService.getPayment(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType), id)
        }
        response.body() ?: throw ApiException("Empty response body", response)
    }

    suspend fun logErrorEvent(errorEvent: ErrorEvent): Unit =
        withContext(coroutineContext) {
            SafeApiRequest.apiRequest {
                val apiResult = sessionManager.getSession()
                if (apiResult is Resource.Error) {
                    throw apiResult.toApiException()
                }

                documentService.logErrorEvent(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType), errorEvent)
            }
        }
}
