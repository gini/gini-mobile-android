package net.gini.android.core.api

import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.gini.android.core.api.Resource.Companion.wrapInResource
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.models.Box
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.Extraction
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.Payment
import net.gini.android.core.api.models.PaymentRequest
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.core.api.models.toPaymentRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Internal use only.
 */
abstract class DocumentRepository<E: ExtractionsContainer>(
    private val documentRemoteSource: DocumentRemoteSource,
    protected val sessionManager: SessionManager,
    private val giniApiType: GiniApiType
) {

    /*
    We need mutex lock because otherwise when the user upload multiple documents at first use of the app, the app
    creates multiple users (equal to number of documents) and we will get a server error (the document does not belong to the user)!
    We are using a Mutex to prevent coroutines from retrieving access tokens in parallel. This way even when multiple uploads are started only one user is created.
     */
    val accessTokenMutex = Mutex()

    suspend fun deletePartialDocumentAndParents(documentId: String): Resource<Unit> =
        withAccessToken { accessToken ->
            wrapInResource {
                val document = getDocumentInternal(accessToken, documentId)
                for (uri in document.compositeDocuments) {
                    documentRemoteSource.deleteDocument(accessToken, uri)
                }
                documentRemoteSource.deleteDocument(accessToken, document.id)
            }
        }

    suspend fun deleteDocument(documentId: String): Resource<Unit> =
        withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.deleteDocument(accessToken, documentId)
            }
        }

    private suspend fun getDocumentInternal(accessToken: String, uri: Uri): Document =
        Document.fromApiResponse(JSONObject(documentRemoteSource.getDocumentFromUri(accessToken, uri)))

    private suspend fun getDocumentInternal(accessToken: String, documentId: String): Document =
        Document.fromApiResponse(JSONObject(documentRemoteSource.getDocument(accessToken, documentId)))

    suspend fun createPartialDocument(documentData: ByteArray, contentType: String,
                                      filename: String? = null,
                                      documentType: DocumentManager.DocumentType? = null,
                                      documentMetadata: DocumentMetadata? = null): Resource<Document> =
        withAccessToken { accessToken ->
            wrapInResource {
                val uri = documentRemoteSource.uploadDocument(
                    accessToken,
                    documentData,
                    MediaTypes.forPartialDocument(giniApiType.giniPartialMediaType, contentType),
                    filename,
                    documentType?.apiDoctypeHint,
                    documentMetadata?.metadata
                )
                getDocumentInternal(accessToken, uri)
            }
        }

    suspend fun createCompositeDocument(documents: List<Document>, documentType: DocumentManager.DocumentType?): Resource<Document> =
        withAccessToken { accessToken ->
            wrapInResource {
                val uri = documentRemoteSource.uploadDocument(
                    accessToken,
                    createCompositeJson(documents),
                    giniApiType.giniCompositeJsonMediaType,
                    null,
                    documentType?.apiDoctypeHint,
                    null
                )
                getDocumentInternal(accessToken, uri)
            }
        }

    suspend fun createCompositeDocument(documentRotationMap: LinkedHashMap<Document, Int>, documentType: DocumentManager.DocumentType?): Resource<Document> =
        withAccessToken { accessToken ->
            wrapInResource {
                val uri = documentRemoteSource.uploadDocument(
                    accessToken,
                    createCompositeJson(documentRotationMap),
                    giniApiType.giniCompositeJsonMediaType,
                    null,
                    documentType?.apiDoctypeHint,
                    null
                )
                getDocumentInternal(accessToken, uri)
            }
        }

    suspend fun getDocument(documentId: String): Resource<Document> =
        withAccessToken { accessToken ->
            wrapInResource {
                Document.fromApiResponse(JSONObject(documentRemoteSource.getDocument(accessToken, documentId)))
            }
        }

    suspend fun getDocument(uri: Uri): Resource<Document> =
        withAccessToken { accessToken ->
            wrapInResource {
                getDocumentInternal(accessToken, uri)
            }
        }

    @Throws(Exception::class)
    abstract fun createExtractionsContainer(specificExtractions: Map<String, SpecificExtraction>,
                                            compoundExtractions: Map<String, CompoundExtraction>,
                                            responseJSON: JSONObject): E

    suspend fun getAllExtractions(document: Document): Resource<E> {
        return withAccessToken { accessToken ->
            wrapInResource {
                val extractionsJSONObject = JSONObject(documentRemoteSource.getExtractions(accessToken, document.id))
                val candidates = extractionCandidatesFromApiResponse(extractionsJSONObject.getJSONObject("candidates"))
                val specificExtractions =
                    parseSpecificExtractions(extractionsJSONObject.getJSONObject("extractions"), candidates)
                val compoundExtractions =
                    parseCompoundExtractions(extractionsJSONObject.optJSONObject("compoundExtractions"), candidates)

                createExtractionsContainer(specificExtractions, compoundExtractions, extractionsJSONObject)
            }
        }
    }

    suspend fun pollDocument(document: Document): Resource<Document> {
        if (document.state != Document.ProcessingState.PENDING) {
            return Resource.Success(document)
        }

        val startTimestamp = System.currentTimeMillis()
        do {
            when (val apiDocumentResource = getDocument(document.id)) {
                is Resource.Success -> {
                    if (apiDocumentResource.data?.state != Document.ProcessingState.PENDING) {
                        return apiDocumentResource
                    }
                }

                is Resource.Cancelled, is Resource.Error -> return apiDocumentResource
            }

            val endTimeStamp = System.currentTimeMillis()
            if (endTimeStamp - startTimestamp > POLLING_TIMEOUT) {
                return Resource.Error(message = "Polling timeout")
            }
            delay(POLLING_INTERVAL)
        } while (true)
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

    suspend fun getLayout(document: Document): Resource<JSONObject> {
        return withAccessToken { accessToken ->
            wrapInResource {
                val layoutJsonString = documentRemoteSource.getLayout(accessToken, document.id)
                JSONObject(layoutJsonString)
            }
        }
    }

    suspend fun getFile(location: String): Resource<ByteArray> =
        withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.getFile(accessToken, location)
            }
        }

    suspend fun getPaymentRequest(id: String): Resource<PaymentRequest> {
        return withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.getPaymentRequest(accessToken, id).toPaymentRequest()
            }
        }
    }

    suspend fun getPaymentRequests(): Resource<List<PaymentRequest>> {
        return withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.getPaymentRequests(accessToken).map { it.toPaymentRequest() }
            }
        }
    }

    suspend fun getPayment(id: String): Resource<Payment> =
        withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.getPayment(accessToken, id)
            }
        }

    @Throws(JSONException::class)
    fun parseSpecificExtractions(specificExtractionsJson: JSONObject, candidates: Map<String, List<Extraction>>): Map<String, SpecificExtraction> {
        val specificExtractions = mutableMapOf<String, SpecificExtraction>()
        val extractionsNameIterator = specificExtractionsJson.keys()
        while (extractionsNameIterator.hasNext()) {
            val extractionName = extractionsNameIterator.next()
            val extractionData = specificExtractionsJson.getJSONObject(extractionName)
            val extraction: Extraction = extractionFromApiResponse(extractionData)
            var candidatesForExtraction = listOf<Extraction?>()
            if (extractionData.has("candidates")) {
                val candidatesName = extractionData.getString("candidates")
                if (candidates.containsKey(candidatesName)) {
                    candidatesForExtraction = candidates[candidatesName]!!
                }
            }
            val specificExtraction = SpecificExtraction(
                extractionName, extraction.value,
                extraction.entity, extraction.box,
                candidatesForExtraction
            )
            specificExtractions[extractionName] = specificExtraction
        }

        return specificExtractions
    }

    @Throws(JSONException::class)
    protected fun parseCompoundExtractions(compoundExtractionsJson: JSONObject?, candidates: Map<String, List<Extraction>>): Map<String, CompoundExtraction> {
        if (compoundExtractionsJson == null) {
            return emptyMap()
        }
        val compoundExtractions = HashMap<String, CompoundExtraction>()
        val extractionsNameIterator = compoundExtractionsJson.keys()
        while (extractionsNameIterator.hasNext()) {
            val extractionName = extractionsNameIterator.next()
            val specificExtractionMaps: MutableList<Map<String, SpecificExtraction>> = ArrayList()
            val compoundExtractionData = compoundExtractionsJson.getJSONArray(extractionName)
            for (i in 0 until compoundExtractionData.length()) {
                val specificExtractionsData = compoundExtractionData.getJSONObject(i)
                specificExtractionMaps.add(parseSpecificExtractions(specificExtractionsData, candidates))
            }
            compoundExtractions[extractionName] = CompoundExtraction(extractionName, specificExtractionMaps)
        }
        return compoundExtractions
    }

    /**
     * Helper method which takes the JSON response of the Gini API as input and returns a mapping where the key is the
     * name of the candidates list (e.g. "amounts" or "dates") and the value is a list of extraction instances.
     *
     * @param responseData The JSON data of the key candidates from the response of the Gini API.
     * @return The created mapping as described above.
     * @throws JSONException If the JSON data does not have the expected structure or if there is invalid data.
     */
    @Throws(JSONException::class)
    protected fun extractionCandidatesFromApiResponse(responseData: JSONObject): HashMap<String, List<Extraction>> {
        val candidatesByEntity = java.util.HashMap<String, List<Extraction>>()
        val entityNameIterator = responseData.keys()
        while (entityNameIterator.hasNext()) {
            val entityName = entityNameIterator.next()
            val candidatesListData = responseData.getJSONArray(entityName)
            val candidates = java.util.ArrayList<Extraction>()
            var i = 0
            val length = candidatesListData.length()
            while (i < length) {
                val extractionData = candidatesListData.getJSONObject(i)
                candidates.add(extractionFromApiResponse(extractionData))
                i += 1
            }
            candidatesByEntity[entityName] = candidates
        }
        return candidatesByEntity
    }

    /**
     * Helper method which creates an Extraction instance from the JSON data which is returned by the Gini API.
     *
     * @param responseData The JSON data.
     * @return The created Extraction instance.
     * @throws JSONException If the JSON data does not have the expected structure or if there is invalid data.
     */
    @Throws(JSONException::class)
    protected open fun extractionFromApiResponse(responseData: JSONObject): Extraction {
        val entity = responseData.getString("entity")
        val value = responseData.getString("value")
        // The box is optional for some extractions.
        var box: Box? = null
        if (responseData.has("box")) {
            box = Box.fromApiResponse(responseData.getJSONObject("box"))
        }
        return Extraction(value, entity, box)
    }

    @Throws(JSONException::class)
    private fun createCompositeJson(documents: List<Document>): ByteArray {
        val documentRotationMap = linkedMapOf<Document, Int>()
        for (document in documents) {
            documentRotationMap[document] = 0
        }

        return createCompositeJson(documentRotationMap)
    }

    @Throws(JSONException::class)
    private fun createCompositeJson(documentRotationMap: LinkedHashMap<Document, Int>): ByteArray {
        val jsonObject = JSONObject()
        val partialDocuments = JSONArray()

        for (entry in documentRotationMap.entries) {
            val document = entry.key
            var rotation = entry.value

            rotation = ((rotation % 360) + 360) % 360
            val partialDoc = JSONObject()
            partialDoc.put("document", document.uri)
            partialDoc.put("rotationDelta", rotation)
            partialDocuments.put(partialDoc)
        }

        jsonObject.put("partialDocuments", partialDocuments)

        return jsonObject.toString().toByteArray(Utils.CHARSET_UTF8)
    }

    protected suspend inline fun <T> withAccessToken(crossinline block: suspend (String) -> Resource<T>): Resource<T> {
        return accessTokenMutex.withLock {
            return@withLock when (val getSession = sessionManager.getSession()) {
                is Resource.Cancelled -> Resource.Cancelled()
                is Resource.Error -> Resource.Error(getSession)
                is Resource.Success -> block(getSession.data.accessToken)
            }
        }
    }

    companion object {
        /**
         * The time in milliseconds between HTTP requests when a document is polled.
         */
        const val POLLING_INTERVAL = 1000L
        /**
         * The time in milliseconds until polling is retried.
         */
        const val POLLING_TIMEOUT = 60000L

        /**
         * The default compression rate which is used for JPEG compression in per cent.
         */
        const val DEFAULT_COMPRESSION = 50

    }
}
