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
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
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
                throw apiResult.toApiException()
            }
            val body: RequestBody = data.toRequestBody("application/octet-stream".toMediaTypeOrNull(), 0, data.size)
            documentService.uploadDocument(customBearerHeaderMap(apiResult.data, metadata, contentType), body, filename, docType)
        }

        Uri.parse(response.headers()[HEADER_LOCATION_KEY] ?: "")
    }

    suspend fun deleteDocument(documentUri: Uri): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw apiResult.toApiException()
            }

            documentService.deleteDocumentFromUri(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType), documentUri)
        }
    }

    suspend fun deleteDocument(documentId: String): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw apiResult.toApiException()
            }

            documentService.deleteDocument(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType), documentId)
        }
    }

    suspend fun getDocument(documentId: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw apiResult.toApiException()
            }
            documentService.getDocument(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType), documentId)
        }
        response.body()?.string() ?: throw ApiException("Empty response body", response)
    }

    suspend fun getDocumentFromUri(uri: Uri): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw apiResult.toApiException()
            }
            documentService.getDocumentFromUri(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType), uriRelativeToBaseUri(uri).toString())
        }
        response.body()?.string() ?: throw ApiException("Empty response body", response)
    }

    suspend fun getExtractions(documentId: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw apiResult.toApiException()
            }
            documentService.getExtractions(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType), documentId)
        }
        response.body()?.string() ?: throw ApiException("Empty response body", response)
    }

    suspend fun errorReportForDocument(documentId: String, summary: String?, description: String?): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw apiResult.toApiException()
            }
            documentService.errorReportForDocument(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType), documentId, summary, description)
        }
        response.body()?.string() ?: throw ApiException("Empty response body", response)
    }

    suspend fun getLayout(documentId: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw apiResult.toApiException()
            }
            documentService.getLayoutForDocument(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType), documentId)
        }
        response.body()?.string() ?: throw ApiException("Empty response body", response)
    }

    suspend fun getFile(location: String): ByteArray = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw apiResult.toApiException()
            }
            documentService.getFile(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType), location)
        }
        response.body() ?: throw ApiException("Empty response body", response)
    }

    suspend fun getPaymentRequest(id: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw apiResult.toApiException()
            }
            documentService.getPaymentRequest(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType), id)
        }
        response.body()?.string() ?: throw ApiException("Empty response body", response)
    }

    suspend fun getPaymentRequests(): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val apiResult = sessionManager.getSession()
            if (apiResult is Resource.Error) {
                throw apiResult.toApiException()
            }
            documentService.getPaymentRequests(bearerHeaderMap(apiResult.data, giniApiType.giniJsonMediaType))
        }
        response.body()?.string() ?: throw ApiException("Empty response body", response)
    }

    protected fun bearerHeaderMap(sessionToken: SessionToken?, contentType: String): Map<String, String> {
        return mapOf("Accept" to giniApiType.giniJsonMediaType,
            "Authorization" to "BEARER ${sessionToken?.accessToken}",
            "Content-Type" to contentType)
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
        const val HEADER_LOCATION_KEY = "location"
    }

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
}
