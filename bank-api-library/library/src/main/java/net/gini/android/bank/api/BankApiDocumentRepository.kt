package net.gini.android.bank.api

import net.gini.android.bank.api.models.*
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.Resource
import net.gini.android.core.api.Resource.Companion.wrapInResource
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.Extraction
import net.gini.android.core.api.models.SpecificExtraction
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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

    @Throws(JSONException::class)
    suspend fun sendFeedbackForExtractions(document: Document, extractions: Map<String, SpecificExtraction>): Resource<Unit> {
        val feedbackForExtractions = JSONObject()
        for (entry in extractions.entries) {
            val extraction = entry.value
            val extractionData = JSONObject()
            extractionData.put("value", extraction.value)
            extractionData.put("entity", extraction.entity)
            feedbackForExtractions.put(entry.key, extractionData)
        }

        val bodyJSON = JSONObject()
        bodyJSON.put("feedback", feedbackForExtractions)
        val body: RequestBody = bodyJSON.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        return withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.sendFeedback(accessToken, document.id, body)
            }
        }
    }

    @Throws(JSONException::class)
    suspend fun sendFeedbackForExtractions(document: Document, extractions: Map<String, SpecificExtraction>, compoundExtractions: Map<String, CompoundExtraction>): Resource<Unit> {
        val feedbackForExtractions = JSONObject()
        for (entry in extractions.entries) {
            val extraction = entry.value
            val extractionData = JSONObject()
            extractionData.put("value", extraction.value)
            extractionData.put("entity", extraction.entity)
            feedbackForExtractions.put(entry.key, extractionData)
        }

        val feedbackForCompoundExtractions = JSONObject()
        for (compoundExtractionEntry in compoundExtractions.entries) {
            val compoundExtraction: CompoundExtraction = compoundExtractionEntry.value
            val specificExtractionsFeedbackObjects = JSONArray()
            for (specificExtractionMap in compoundExtraction.specificExtractionMaps) {
                val specificExtractionsFeedback = JSONObject()
                for (specificExtractionEntry in specificExtractionMap.entries) {
                    val extraction: Extraction = specificExtractionEntry.value
                    val extractionData = JSONObject()
                    extractionData.put("value", extraction.value)
                    extractionData.put("entity", extraction.entity)
                    specificExtractionsFeedback.put(specificExtractionEntry.key, extractionData)
                }
                specificExtractionsFeedbackObjects.put(specificExtractionsFeedback)
            }
            feedbackForCompoundExtractions.put(compoundExtractionEntry.key, specificExtractionsFeedbackObjects)
        }

        val bodyJSON = JSONObject()
        bodyJSON.put("extractions", feedbackForExtractions)
        bodyJSON.put("compoundExtractions", feedbackForCompoundExtractions)
        val body: RequestBody = bodyJSON.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        return withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.sendFeedback(accessToken, document.id, body)
            }
        }
    }

    suspend fun resolvePaymentRequest(requestId: String, resolvePaymentInput: ResolvePaymentInput): Resource<ResolvedPayment> =
        withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.resolvePaymentRequests(accessToken, requestId, resolvePaymentInput)
            }
        }

    suspend fun getPayment(id: String): Resource<Payment> =
        withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.getPayment(accessToken, id)
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