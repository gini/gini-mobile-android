package net.gini.android.core.api

import android.net.Uri
import kotlinx.coroutines.withContext
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.requests.SafeApiRequest
import retrofit2.http.HEAD
import retrofit2.http.Header
import kotlin.coroutines.CoroutineContext

class DocumentRemoteSource(
    val coroutineContext: CoroutineContext,
    private val documentService: DocumentService,
    private val giniApiType: GiniApiType,
    private val sessionToken: SessionToken,
    baseUriString: String
) {

    private var baseUri: Uri? = null

    init {
        baseUri = getBaseUri(baseUriString, giniApiType)
    }

    suspend fun uploadDocument(data: ByteArray, contentType: String ,filename: String?, docType: String?, metadata: Map<String, String>?): Uri = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.uploadDocument(customBearerHeaderMap(metadata, contentType), data, filename, docType)
        }
        Uri.parse(response.second[HEADER_LOCATION_KEY]?.first() ?: "")
    }

    suspend fun deleteDocument(documentUri: Uri): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.deleteDocumentFromUri(bearerHeaderMap(), documentUri)
        }
        response.first
    }

    suspend fun deleteDocument(documentId: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.deleteDocument(bearerHeaderMap(), documentId)
        }
        response.first
    }

    suspend fun getDocument(documentId: String): Document = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getDocument(bearerHeaderMap(), documentId)
        }
        response.first
    }

    suspend fun getDocumentFromUri(uri: Uri): Document = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getDocumentFromUri(bearerHeaderMap(), uriRelativeToBaseUri(uri).toString())
        }
        response.first
    }

    private fun bearerHeaderMap(): Map<String, String> {
        return mapOf("Accept" to giniApiType.giniJsonMediaType,
            "Authorization" to "BEARER $sessionToken")
    }

    private fun customBearerHeaderMap(metadata: Map<String, String>?, contentType: String): Map<String, String> {
        var customHeader = mapOf<String, String>()
        customHeader = customHeader +
                mapOf("Accept" to giniApiType.giniJsonMediaType,
                    "Authorization" to "BEARER $sessionToken",
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
