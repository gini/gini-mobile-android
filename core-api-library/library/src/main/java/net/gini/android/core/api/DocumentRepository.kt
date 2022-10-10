package net.gini.android.core.api

import android.net.Uri
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.*
import net.gini.android.core.api.Resource.Companion.wrapInResource
import net.gini.android.core.api.models.*
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.response.PaymentRequestResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

abstract class DocumentRepository<E: ExtractionsContainer>(
    private val documentRemoteSource: DocumentRemoteSource,
    private val giniApiType: GiniApiType,
    private val moshi: Moshi
) {

    private var mDocumentPollingsInProgress: Map<Document, Boolean> = ConcurrentHashMap()

    suspend fun deletePartialDocumentAndParents(documentId: String): Resource<Unit> =
        wrapInResource {
            val document = getDocumentInternal(documentId)
            for (uri in document.compositeDocuments) {
                documentRemoteSource.deleteDocument(uri)
            }
            documentRemoteSource.deleteDocument(document.id)
        }

    suspend fun deleteDocument(documentId: String): Resource<Unit> =
        wrapInResource {
            documentRemoteSource.deleteDocument(documentId)
        }

    private suspend fun getDocumentInternal(uri: Uri): Document =
        Document.fromApiResponse(JSONObject(documentRemoteSource.getDocumentFromUri(uri)))

    private suspend fun getDocumentInternal(documentId: String): Document =
        Document.fromApiResponse(JSONObject(documentRemoteSource.getDocument(documentId)))

    suspend fun createPartialDocument(documentData: ByteArray, contentType: String,
                                      filename: String? = null,
                                      documentType: DocumentRemoteSource.DocumentType? = null,
                                      documentMetadata: DocumentMetadata? = null): Resource<Document> {
        val apiDoctypeHint = documentType?.apiDoctypeHint

        return wrapInResource {
            val uri = documentRemoteSource.uploadDocument(
                documentData,
                MediaTypes.forPartialDocument(giniApiType.giniPartialMediaType, contentType),
                filename,
                apiDoctypeHint,
                documentMetadata?.metadata
            )
            getDocumentInternal(uri)
        }
    }

    /**
     * Creates a new Gini composite document.
     *
     * @param documents    A list of partial documents which should be part of a multi-page document
     * @param documentType Optional a document type hint. See the documentation for the document type hints for
     *                     possible values
     * @return A Resource which has in the "data" the freshly created document.
     */

    suspend fun createCompositeDocument(documents: List<Document>, documentType: DocumentRemoteSource.DocumentType?): Resource<Document> {
        val apiDoctypeHint = documentType?.apiDoctypeHint

        return wrapInResource {
            val uri = documentRemoteSource.uploadDocument(
                createCompositeJson(documents),
                giniApiType.giniCompositeJsonMediaType,
                null,
                apiDoctypeHint,
                null
            )
            getDocumentInternal(uri)
        }
    }

    /**
     * Creates a new Gini composite document. The input Map must contain the partial documents as keys. These will be
     * part of the multi-page document. The value for each partial document key is the amount in degrees the document
     * has been rotated by the user.
     *
     * @param documentRotationMap A map of partial documents and their rotation in degrees
     * @param documentType        Optional a document type hint. See the documentation for the document type hints for
     *                            possible values
     * @return A Resource which has in the "data" the freshly created document.
     */

    suspend fun createCompositeDocument(documentRotationMap: LinkedHashMap<Document, Int>, documentType: DocumentRemoteSource.DocumentType?): Resource<Document> {
        val apiDoctypeHint = documentType?.apiDoctypeHint

        return wrapInResource {
            val uri = documentRemoteSource.uploadDocument(
                createCompositeJson(documentRotationMap),
                giniApiType.giniCompositeJsonMediaType,
                null,
                apiDoctypeHint,
                null
            )
            getDocumentInternal(uri)
        }
    }

    /**
     * Uploads raw data and creates a new Gini document.
     *
     * @param document     A byte array representing an image, a pdf or UTF-8 encoded text
     * @param filename     Optional the filename of the given document.
     * @param documentType Optional a document type hint. See the documentation for the document type hints for
     *                     possible values.
     * @return A Resource with the Document instance of the freshly created document or informations about error
     *
     * <b>Important:</b> If using the default Gini API, then use {@link #createPartialDocument(byte[], String, String, DocumentType)} to upload the
     * document and then call {@link #createCompositeDocument(LinkedHashMap, DocumentType)}
     * (or {@link #createCompositeDocument(List, DocumentType)}) to finish document creation. The
     * returned composite document can be used to poll the processing state, to retrieve extractions
     * and to send feedback.
     */

    suspend fun createDocument(document: ByteArray, filename: String?, documentType: DocumentRemoteSource.DocumentType?): Resource<Document> {
        return createDocumentInternal(document, filename, documentType, null)
    }

    /**
     * Uploads raw data and creates a new Gini document.
     *
     * @param document         A byte array representing an image, a pdf or UTF-8 encoded text
     * @param filename         Optional the filename of the given document.
     * @param documentType     Optional a document type hint. See the documentation for the document type hints for
     *                         possible values.
     * @param documentMetadata Additional information related to the document (e.g. the branch id
     *                         to which the client app belongs)
     * @return A Resource with the Document instance of the freshly created document or informations about error.
     *
     * <b>Important:</b> If using the default Gini API, then use {@link #createPartialDocument(byte[], String, String, DocumentType)} to upload the
     * document and then call {@link #createCompositeDocument(LinkedHashMap, DocumentType)}
     * (or {@link #createCompositeDocument(List, DocumentType)}) to finish document creation. The
     * returned composite document can be used to poll the processing state, to retrieve extractions
     * and to send feedback.
     */

    suspend fun createDocument(document: ByteArray, filename: String?, documentType: DocumentRemoteSource.DocumentType?, documentMetadata: DocumentMetadata): Resource<Document> {
        return createDocumentInternal(document, filename, documentType, documentMetadata)
    }

    private suspend fun createDocumentInternal(document: ByteArray, filename: String?, documentType: DocumentRemoteSource.DocumentType?, documentMetadata: DocumentMetadata?): Resource<Document> {
        val apiDoctypeHint: String? = documentType?.apiDoctypeHint
        return wrapInResource {
            val uri = documentRemoteSource.uploadDocument(document, MediaTypes.IMAGE_JPEG, filename, apiDoctypeHint, documentMetadata?.metadata)
            getDocumentInternal(uri)
        }
    }

    /**
     * Get the document with the given unique identifier.
     *
     * @param documentId The unique identifier of the document.
     * @return A Resource document instance representing all the document's metadata.
     */
    suspend fun getDocument(documentId: String): Resource<Document> =
        wrapInResource {
            Document.fromApiResponse(JSONObject(documentRemoteSource.getDocument(documentId)))
        }

    suspend fun getDocument(uri: Uri): Resource<Document> =
        wrapInResource {
            getDocumentInternal(uri)
        }

    @Throws(Exception::class)
    abstract fun createExtractionsContainer(specificExtractions: Map<String, SpecificExtraction>,
                                            compoundExtractions: Map<String, CompoundExtraction>,
                                            responseJSON: JSONObject): E


    /**
     * Get the extractions for the given document.
     *
     * @param document The Document instance for whose document the extractions are returned.
     * @return A Task which will resolve to an {@link ExtractionsContainer} object.
     */
    /**
     * Get the extractions for the given document.
     *
     * @param document The Document instance for whose document the extractions are returned.
     * @return A Resource with an {@link ExtractionsContainer} object.
     */
    @Throws(ApiException::class)
    suspend fun getAllExtractions(document: Document): Resource<E> {
        return wrapInResource {
            val extractionsJSONObject = JSONObject(documentRemoteSource.getExtractions(document.id))
            val candidates = extractionCandidatesFromApiResponse(extractionsJSONObject.getJSONObject("candidates"))
            val specificExtractions = parseSpecificExtractions(extractionsJSONObject.getJSONObject("extractions"), candidates)
            val compoundExtractions = parseCompoundExtractions(extractionsJSONObject.optJSONObject("compoundExtractions"), candidates)

            createExtractionsContainer(specificExtractions, compoundExtractions, extractionsJSONObject)
        }
    }

    /**
     * Continually checks the document status (via the Gini API) until the document is fully processed. To avoid
     * flooding the network, there is a pause of at least the number of seconds that is set in the POLLING_INTERVAL
     * constant of this class.
     *
     * <b>This method returns a Resource with a new document instance. It does not update the given
     * document instance.</b>
     *
     * @param document The document which will be polled.
     */
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

    /**
     * Sends an error report for the given document to Gini. If the processing result for a document was not
     * satisfactory (e.g. extractions where empty or incorrect), you can create an error report for a document. This
     * allows Gini to analyze and correct the problem that was found.
     *
     * <b>The owner of this document must agree that Gini can use this document for debugging and error analysis.</b>
     *
     * @param document    The erroneous document.
     * @param summary     Optional a short summary of the occurred error.
     * @param description Optional a more detailed description of the occurred error.
     * @return A Resource which will resolve to an error ID. This is a unique identifier for your error report
     * and can be used to refer to the reported error towards the Gini support.
     */
    @Throws(ApiException::class)
    suspend fun reportDocument(document: Document, summary: String?, description: String?): Resource<String> {
        return wrapInResource {
            val response = documentRemoteSource.errorReportForDocument(document.id, summary, description)
            val jsonObject = JSONObject(response)
            jsonObject.getString("errorId")
        }
    }

    /**
     * Gets the layout of a document. The layout of the document describes the textual content of a document with
     * positional information, based on the processed document.
     *
     * @param document The document for which the layouts is requested.
     * @return A Resource with a string containing the layout xml or error data
     */
    @Throws(ApiException::class)
    suspend fun getLayout(document: Document): Resource<String?> {
        return wrapInResource {
            documentRemoteSource.getLayout(document.id)
        }
    }

    /**
     * Download a file.
     *
     * @return byte array of file contents
     */
    suspend fun getFile(location: String): Resource<ByteArray> =
        wrapInResource {
            documentRemoteSource.getFile(location)
        }

    /**
     * @return Resource with {PaymentRequest} for the given id
     */
    @Throws(ApiException::class)
    suspend fun getPaymentRequest(id: String): Resource<PaymentRequest?> {
        return wrapInResource {
            val response = documentRemoteSource.getPaymentRequest(id)
            val adapter: JsonAdapter<PaymentRequestResponse> = moshi.adapter(PaymentRequestResponse::class.java)
            val requestResponse = adapter.fromJson(JSONObject(response).toString())
            requestResponse?.toPaymentRequest()
        }
    }

    /**
     * @return List of payment {@link PaymentRequest}
     */
    @Throws(ApiException::class)
    suspend fun getPaymentRequests(): Resource<List<PaymentRequest>> {
        return wrapInResource {
            val response = documentRemoteSource.getPaymentRequests()
            val type: Type = Types.newParameterizedType(MutableList::class.java, PaymentRequestResponse::class.java)
            val adapter: JsonAdapter<List<PaymentRequestResponse>> = moshi.adapter(type)
            val requestResponses = adapter.fromJson(JSONArray(response).toString())

            val paymentProviders = mutableListOf<PaymentRequest>()
            for (paymentRequestResponse in requestResponses ?: emptyList()) {
                paymentProviders.add(paymentRequestResponse.toPaymentRequest())
            }
            paymentProviders
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

    companion object {
        /**
         * The time in milliseconds between HTTP requests when a document is polled.
         */
        const val POLLING_INTERVAL = 1000L
        const val POLLING_TIMEOUT = 60000L

        /**
         * The default compression rate which is used for JPEG compression in per cent.
         */
        const val DEFAULT_COMPRESSION = 50

    }
}
