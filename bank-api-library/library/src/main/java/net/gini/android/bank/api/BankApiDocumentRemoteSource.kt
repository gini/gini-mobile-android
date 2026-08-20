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

    suspend fun resolvePaymentRequests(id: String, input: ResolvePaymentInput): ResolvedPayment =
        resolvePaymentRequests(headerMap(contentType = giniApiType.giniJsonMediaType), id, input)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun resolvePaymentRequests(accessToken: String, id: String, input: ResolvePaymentInput): ResolvedPayment =
        resolvePaymentRequests(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), id, input)

    private suspend fun resolvePaymentRequests(headers: Map<String, String>, id: String, input: ResolvePaymentInput)
            : ResolvedPayment = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.resolvePaymentRequests(headers, id, input.toResolvePaymentBody())
        }
        response.body()?.toResolvedPayment()
            ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun logErrorEvent(errorEvent: ErrorEvent): Unit =
        logErrorEvent(headerMap(contentType = giniApiType.giniJsonMediaType), errorEvent)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun logErrorEvent(accessToken: String, errorEvent: ErrorEvent): Unit =
        logErrorEvent(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), errorEvent)

    private suspend fun logErrorEvent(headers: Map<String, String>, errorEvent: ErrorEvent): Unit =
        withContext(coroutineContext) {
            SafeApiRequest.apiRequest {
                documentService.logErrorEvent(headers, errorEvent)
            }
        }

    suspend fun getConfigurations(): Configuration =
        getConfigurations(headerMap(contentType = giniApiType.giniJsonMediaType))

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun getConfigurations(accessToken: String): Configuration =
        getConfigurations(bearerHeaderMap(accessToken, giniApiType.giniJsonMediaType))

    private suspend fun getConfigurations(headers: Map<String, String>): Configuration =
        withContext(coroutineContext) {
            val response = SafeApiRequest.apiRequest {
                documentService.getConfigurations(headers)
            }
            response.body()?.toConfiguration()
                ?: throw ApiException.forResponse("Empty response body", response)
        }

    private companion object {
        private const val ACCESS_TOKEN_DEPRECATION_MESSAGE =
            "The Authorization header is added by the SDK's session interceptor in the OkHttp layer. " +
                    "Use the overload without an accessToken parameter."
    }
}
