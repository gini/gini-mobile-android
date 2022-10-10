package net.gini.android.bank.api

import com.squareup.moshi.Moshi
import net.gini.android.bank.api.models.*
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.Resource
import net.gini.android.core.api.Resource.Companion.wrapInResource
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.Extraction
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.core.api.requests.ApiException
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

class BankApiDocumentRepository(
    override val coroutineContext: CoroutineContext,
    private val documentRemoteSource: BankApiDocumentRemoteSource,
    private val giniApiType: GiniBankApiType,
    private val moshi: Moshi
) : DocumentRepository<ExtractionsContainer>(coroutineContext, documentRemoteSource, giniApiType, moshi) {

    override fun createExtractionsContainer(
        specificExtractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>,
        responseJSON: JSONObject
    ): ExtractionsContainer {
        val returnReasons: List<ReturnReason> = parseReturnReason(responseJSON.optJSONArray("returnReasons"))

        return ExtractionsContainer(specificExtractions, compoundExtractions, returnReasons)
    }

    /**
     * Sends approved and conceivably corrected extractions for the given document. This is called "submitting feedback
     * on extractions" in
     * the Gini API documentation.
     *
     * @param document    The document for which the extractions should be updated.
     * @param extractions A Map where the key is the name of the specific extraction and the value is the
     *                    SpecificExtraction object. This is the same structure as returned by the getExtractions
     *                    method of this manager.
     *
     * @return A Task which will resolve to the same document instance when storing the updated
     * extractions was successful.
     *
     * @throws JSONException When a value of an extraction is not JSON serializable.
     */

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
        return wrapInResource {
            documentRemoteSource.sendFeedback(document.id, body)
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
        return wrapInResource {
            documentRemoteSource.sendFeedback(document.id, body)
        }
    }

    /**
     * Mark a {@link PaymentRequest} as paid.
     *
     * @param requestId id of request
     * @param resolvePaymentInput information of the actual payment
     */
    suspend fun resolvePaymentRequest(requestId: String, resolvePaymentInput: ResolvePaymentInput): Resource<ResolvedPayment> =
        wrapInResource {
            documentRemoteSource.resolvePaymentRequests(requestId, resolvePaymentInput)
        }

    /**
     * Get information about the payment of the {@link PaymentRequest}
     *
     * @param id of the paid {@link PaymentRequest}
     */
    suspend fun getPayment(id: String): Resource<Payment> =
        wrapInResource {
            documentRemoteSource.getPayment(id)
        }

    suspend fun logErrorEvent(errorEvent: ErrorEvent): Resource<Unit> =
        wrapInResource {
            documentRemoteSource.logErrorEvent(errorEvent)
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