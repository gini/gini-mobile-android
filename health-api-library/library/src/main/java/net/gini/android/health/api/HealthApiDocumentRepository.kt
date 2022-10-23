package net.gini.android.health.api

import android.net.Uri
import android.util.Size
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.Resource
import net.gini.android.core.api.Resource.Companion.wrapInResource
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.Extraction
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.health.api.models.Page
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.health.api.models.getPageByPageNumber
import net.gini.android.health.api.models.toPageList
import net.gini.android.health.api.models.toPaymentProvider
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Alpár Szotyori on 14.10.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */
class HealthApiDocumentRepository(
    private val documentRemoteSource: HealthApiDocumentRemoteSource,
    sessionManager: SessionManager,
    private val giniApiType: GiniHealthApiType
) : DocumentRepository<ExtractionsContainer>(documentRemoteSource, sessionManager, giniApiType) {

    override fun createExtractionsContainer(
        specificExtractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>,
        responseJSON: JSONObject
    ): ExtractionsContainer = ExtractionsContainer(specificExtractions, compoundExtractions)

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
    suspend fun sendFeedbackForExtractions(
        document: Document,
        extractions: Map<String, SpecificExtraction>
    ): Resource<Unit> {
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
        return withSession { sessionToken ->
            wrapInResource {
                documentRemoteSource.sendFeedback(sessionToken, document.id, body)
            }
        }
    }

    @Throws(JSONException::class)
    suspend fun sendFeedbackForExtractions(
        document: Document,
        extractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>
    ): Resource<Unit> {
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
        return withSession { sessionToken ->
            wrapInResource {
                documentRemoteSource.sendFeedback(sessionToken, document.id, body)
            }
        }
    }

    suspend fun getPageImage(
        documentId: String,
        page: Int
    ): Resource<ByteArray> =
        withSession { sessionToken ->
            wrapInResource {
                val imageUri = getPages(sessionToken, documentId)
                    .getPageByPageNumber(page)
                    .getLargestImageUriSmallerThan(Size(2000, 2000))

                if (imageUri != null) {
                    documentRemoteSource.getFile(sessionToken, imageUri.toString())
                } else {
                    throw NoSuchElementException("No page image found for page number $page in document $documentId")
                }
            }
        }

    private suspend fun getPages(sessionToken: SessionToken, documentId: String): List<Page> =
        documentRemoteSource.getPages(sessionToken, documentId)
            .toPageList(Uri.parse(giniApiType.baseUrl))

    suspend fun getPaymentProviders(): Resource<List<PaymentProvider>> {
        return withSession { sessionToken ->
            wrapInResource {
                documentRemoteSource.getPaymentProviders(sessionToken).map { paymentProviderResponse ->
                    val icon = documentRemoteSource.getFile(sessionToken, paymentProviderResponse.iconLocation)
                    paymentProviderResponse.toPaymentProvider(icon)
                }
            }
        }
    }

    suspend fun getPaymentProvider(providerId: String): Resource<PaymentProvider> =
        withSession { sessionToken ->
            wrapInResource {
                val paymentProviderResponse = documentRemoteSource.getPaymentProvider(sessionToken, providerId)
                val icon = documentRemoteSource.getFile(sessionToken, paymentProviderResponse.iconLocation)
                paymentProviderResponse.toPaymentProvider(icon)
            }
        }

    suspend fun createPaymentRequest(paymentRequestInput: PaymentRequestInput): Resource<String> {
        return withSession { sessionToken ->
            wrapInResource {
                documentRemoteSource.createPaymentRequest(sessionToken, paymentRequestInput)
            }
        }
    }
}