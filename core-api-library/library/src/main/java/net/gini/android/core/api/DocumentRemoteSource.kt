package net.gini.android.core.api

import android.net.Uri
import kotlinx.coroutines.withContext
import net.gini.android.core.api.authorization.KSessionManager
import net.gini.android.core.api.authorization.UserService
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.SafeApiRequest
import net.gini.android.core.api.response.PaymentRequestResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.Query
import kotlin.coroutines.CoroutineContext

abstract class DocumentRemoteSource(
    open val coroutineContext: CoroutineContext,
    private val documentService: DocumentService,
    private val giniApiType: GiniApiType,
    private val sessionManager: KSessionManager,
    baseUriString: String
) {

    private var baseUri: Uri? = null

    init {
        baseUri = getBaseUri(baseUriString, giniApiType)
    }

    suspend fun uploadDocument(data: ByteArray, contentType: String ,filename: String?, docType: String?, metadata: Map<String, String>?): Uri = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw ApiException(apiResult.message, apiResult.responseStatusCode, apiResult.responseBody, apiResult.responseHeaders)
            }
            documentService.uploadDocument(customBearerHeaderMap(apiResult.data, metadata, contentType), data, filename, docType)
        }
        Uri.parse(response.second[HEADER_LOCATION_KEY]?.first() ?: "")
    }

    suspend fun deleteDocument(documentUri: Uri): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw ApiException(apiResult.message, apiResult.responseStatusCode, apiResult.responseBody, apiResult.responseHeaders)
            }

            documentService.deleteDocumentFromUri(bearerHeaderMap(apiResult.data), documentUri)
        }
        response.first
    }

    suspend fun deleteDocument(documentId: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw ApiException(apiResult.message, apiResult.responseStatusCode, apiResult.responseBody, apiResult.responseHeaders)
            }

            documentService.deleteDocument(bearerHeaderMap(apiResult.data), documentId)
        }
        response.first
    }

    suspend fun getDocument(documentId: String): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw ApiException(apiResult.message, apiResult.responseStatusCode, apiResult.responseBody, apiResult.responseHeaders)
            }
            documentService.getDocument(bearerHeaderMap(apiResult.data), documentId)
        }
        response.first
    }

    suspend fun getDocumentFromUri(uri: Uri): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw ApiException(apiResult.message, apiResult.responseStatusCode, apiResult.responseBody, apiResult.responseHeaders)
            }
            documentService.getDocumentFromUri(bearerHeaderMap(apiResult.data), uriRelativeToBaseUri(uri).toString())
        }
        response.first
    }

    suspend fun getExtractions(documentId: String): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw ApiException(apiResult.message, apiResult.responseStatusCode, apiResult.responseBody, apiResult.responseHeaders)
            }
            documentService.getExtractions(bearerHeaderMap(apiResult.data), documentId)
        }
        response.first
    }

    suspend fun errorReportForDocument(documentId: String, summary: String?, description: String?): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw ApiException(apiResult.message, apiResult.responseStatusCode, apiResult.responseBody, apiResult.responseHeaders)
            }
            documentService.errorReportForDocument(bearerHeaderMap(apiResult.data), documentId, summary, description)
        }
        response.first
    }

    suspend fun getLayout(documentId: String): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw ApiException(apiResult.message, apiResult.responseStatusCode, apiResult.responseBody, apiResult.responseHeaders)
            }
            documentService.getLayoutForDocument(bearerHeaderMap(apiResult.data), documentId)
        }
        response.first
    }

    suspend fun getFile(location: String): ByteArray = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw ApiException(apiResult.message, apiResult.responseStatusCode, apiResult.responseBody, apiResult.responseHeaders)
            }
            documentService.getFile(bearerHeaderMap(apiResult.data), location)
        }
        response.first
    }

    suspend fun getPaymentRequest(id: String): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw ApiException(apiResult.message, apiResult.responseStatusCode, apiResult.responseBody, apiResult.responseHeaders)
            }
            documentService.getPaymentRequest(bearerHeaderMap(apiResult.data), id)
        }
        response.first
    }

    suspend fun getPaymentRequests(): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw ApiException(apiResult.message, apiResult.responseStatusCode, apiResult.responseBody, apiResult.responseHeaders)
            }
            documentService.getPaymentRequests(bearerHeaderMap(apiResult.data))
        }
        response.first
    }

    protected  fun bearerHeaderMap(sessionToken: SessionToken?): Map<String, String> {
        return mapOf("Accept" to giniApiType.giniJsonMediaType,
            "Authorization" to "BEARER ${sessionToken?.accessToken}")
    }

    protected fun customBearerHeaderMap(sessionToken: SessionToken?, metadata: Map<String, String>?, contentType: String): Map<String, String> {
        var customHeader = mapOf<String, String>()
        customHeader = customHeader +
                mapOf("Accept" to giniApiType.giniJsonMediaType,
                    "Authorization" to "BEARER ${sessionToken?.accessToken}",
                    "Content-Type" to contentType) as MutableMap<String, String>
        metadata?.let {
            customHeader = customHeader + it
        }

        return customHeader
    }

    private fun uriRelativeToBaseUri(uri: Uri): Uri? {
        return baseUri?.buildUpon()?.path(uri.path)?.query(uri.query)?.build()
    }

    private fun getBaseUri(baseUriString: String?, giniApiType: GiniApiType): Uri? {
        return if (baseUriString != null) {
            Uri.parse(Utils.checkNotNull(baseUriString))
        } else {
            Uri.parse(giniApiType.baseUrl)
        }
    }

    companion object {
        enum class DocumentType(val apiDoctypeHint: String) {
            BANK_STATEMENT("BankStatement"),
            CONTRACT("Contract"),
            INVOICE("Invoice"),
            RECEIPT("Receipt"),
            REMINDER("Reminder"),
            REMITTANCE_SLIP("RemittanceSlip"),
            TRAVEL_EXPENSE_REPORT("TravelExpenseReport"),
            OTHER("Other");
        }
        const val HEADER_LOCATION_KEY = "location"
    }
}
