package net.gini.android.bank.api

import kotlinx.coroutines.withContext
import net.gini.android.bank.api.models.Configuration
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.bank.api.models.toResolvedPayment
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.bank.api.requests.toResolvePaymentBody
import net.gini.android.bank.api.response.toConfiguration
import net.gini.android.core.api.DocumentRemoteSource
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.SafeApiRequest
import kotlin.coroutines.CoroutineContext

/**
 * Internal use only.
 */
class BankApiDocumentRemoteSource internal constructor(
    override var coroutineContext: CoroutineContext,
    private val documentService: BankApiDocumentService,
    private val giniApiType: GiniBankApiType,
    baseUriString: String
) : DocumentRemoteSource(coroutineContext, documentService, giniApiType, baseUriString) {

    suspend fun resolvePaymentRequests(id: String, input: ResolvePaymentInput)
            : ResolvedPayment = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.resolvePaymentRequests(
                buildHeaderMap(
                    contentType = giniApiType.giniJsonMediaType
                ), id, input.toResolvePaymentBody()
            )
        }
        response.body()?.toResolvedPayment()
            ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun logErrorEvent(errorEvent: ErrorEvent): Unit =
        withContext(coroutineContext) {
            SafeApiRequest.apiRequest {
                documentService.logErrorEvent(
                    buildHeaderMap(
                        contentType = giniApiType.giniJsonMediaType
                    ), errorEvent
                )
            }
        }

    suspend fun getConfigurations(): Configuration =
        withContext(coroutineContext) {
            val response = SafeApiRequest.apiRequest {
                documentService.getConfigurations(
                    buildHeaderMap(
                        giniApiType.giniJsonMediaType
                    )
                )
            }
            response.body()?.toConfiguration()
                ?: throw ApiException.forResponse("Empty response body", response)
        }
}
