package net.gini.android.bank.api

import kotlinx.coroutines.withContext
import net.gini.android.bank.api.models.Payment
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.bank.api.models.toPayment
import net.gini.android.bank.api.models.toResolvedPayment
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.bank.api.requests.toResolvePaymentBody
import net.gini.android.core.api.DocumentRemoteSource
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.SafeApiRequest
import okhttp3.RequestBody
import kotlin.coroutines.CoroutineContext

/**
 * Internal use only.
 */
class BankApiDocumentRemoteSource internal constructor(
    override var coroutineContext: CoroutineContext,
    private val documentService: BankApiDocumentService,
    private val giniApiType: GiniBankApiType,
    baseUriString: String
): DocumentRemoteSource(coroutineContext, documentService, giniApiType, baseUriString) {

    suspend fun sendFeedback(accessToken: String, documentId: String, requestBody: RequestBody): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.sendFeedback(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), documentId, requestBody)
        }
    }

    suspend fun resolvePaymentRequests(accessToken: String, id: String, input: ResolvePaymentInput): ResolvedPayment = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.resolvePaymentRequests(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), id, input.toResolvePaymentBody())
        }
        response.body()?.toResolvedPayment() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getPayment(accessToken: String, id: String): Payment = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPayment(bearerHeaderMap(accessToken, giniApiType.giniJsonMediaType), id)
        }
        response.body()?.toPayment() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun logErrorEvent(accessToken: String, errorEvent: ErrorEvent): Unit =
        withContext(coroutineContext) {
            SafeApiRequest.apiRequest {
                documentService.logErrorEvent(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), errorEvent)
            }
        }
}
