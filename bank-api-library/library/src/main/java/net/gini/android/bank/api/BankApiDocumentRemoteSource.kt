package net.gini.android.bank.api

import android.net.Uri
import kotlinx.coroutines.withContext
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.core.api.DocumentRemoteSource
import net.gini.android.core.api.DocumentService
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.KSessionManager
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.SafeApiRequest
import okhttp3.ResponseBody
import kotlin.coroutines.CoroutineContext

class BankApiDocumentRemoteSource(
    override var coroutineContext: CoroutineContext,
    val documentService: BankApiDocumentService,
    private val giniApiType: GiniBankApiType,
    private val sessionManager: KSessionManager,
    baseUriString: String
): DocumentRemoteSource(coroutineContext, documentService, giniApiType, sessionManager, baseUriString) {

    suspend fun sendFeedback(documentId: String): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw ApiException(apiResult.message, apiResult.responseStatusCode, apiResult.responseBody, apiResult.responseHeaders)
            }

            documentService.sendFeedback(bearerHeaderMap(apiResult.data), documentId)
        }
        response.first
    }

    suspend fun resolvePaymentRequests(documentId: String): ResolvedPayment = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw ApiException(apiResult.message, apiResult.responseStatusCode, apiResult.responseBody, apiResult.responseHeaders)
            }

            documentService.resolvePaymentRequests(bearerHeaderMap(apiResult.data), documentId)
        }
        response.first
    }
}
