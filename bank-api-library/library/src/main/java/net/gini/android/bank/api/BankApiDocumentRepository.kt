package net.gini.android.bank.api

import net.gini.android.bank.api.models.*
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.Resource
import net.gini.android.core.api.Resource.Companion.wrapInResource
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.SpecificExtraction
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Internal use only.
 */
class BankApiDocumentRepository(
    private val documentRemoteSource: BankApiDocumentRemoteSource,
    sessionManager: SessionManager,
    giniApiType: GiniBankApiType
) : DocumentRepository<ExtractionsContainer>(documentRemoteSource, sessionManager, giniApiType) {

    override fun createExtractionsContainer(
        specificExtractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>,
        responseJSON: JSONObject
    ): ExtractionsContainer {
        val returnReasons: List<ReturnReason> = parseReturnReason(responseJSON.optJSONArray("returnReasons"))

        return ExtractionsContainer(specificExtractions, compoundExtractions, returnReasons)
    }

    suspend fun resolvePaymentRequest(requestId: String, resolvePaymentInput: ResolvePaymentInput): Resource<ResolvedPayment> =
        withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.resolvePaymentRequests(accessToken, requestId, resolvePaymentInput)
            }
        }

    suspend fun logErrorEvent(errorEvent: ErrorEvent): Resource<Unit> =
        withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.logErrorEvent(accessToken, errorEvent)
            }
        }

    @Throws(JSONException::class)
    private fun parseReturnReason(returnReasonsJson: JSONArray?): List<ReturnReason> {
        if (returnReasonsJson == null) {
            return emptyList()
        }
        val returnReasons: MutableList<ReturnReason> = ArrayList()
        for (i in 0 until returnReasonsJson.length()) {
            val returnReasonJson = returnReasonsJson.getJSONObject(i)
            val localizedLabels: MutableMap<String, String> = HashMap()
            val keys = returnReasonJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key == "id") {
                    continue
                }
                localizedLabels[key] = returnReasonJson.getString(key)
            }
            returnReasons.add(ReturnReason(returnReasonJson.getString("id"), localizedLabels))
        }
        return returnReasons
    }
}