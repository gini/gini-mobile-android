package net.gini.android.core.api

import android.net.Uri
import kotlinx.coroutines.*
import net.gini.android.core.api.models.*
import net.gini.android.core.api.requests.ApiException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

abstract class DocumentRepository<E: ExtractionsContainer>(
    override val coroutineContext: CoroutineContext,
    private val documentRemoteSource: DocumentRemoteSource,
    private val giniApiType: GiniApiType
) : CoroutineScope {

    suspend fun deletePartialDocumentAndParents(documentId: String): Resource<String> =
        wrapResponseIntoResource {
            val document = documentRemoteSource.getDocument(documentId)
            deleteDocuments(document.compositeDocuments)
            documentRemoteSource.deleteDocument(document.id)
        }

    suspend fun deleteDocuments(documents: List<Uri>) {
        val tasks = mutableListOf<Job>()
        for (uri in documents) {
            tasks.add(launch { documentRemoteSource.deleteDocument(uri) })
        }

        tasks.joinAll()
    }

    suspend fun deleteDocument(documentId: String): Resource<String> =
        wrapResponseIntoResource {
            documentRemoteSource.deleteDocument(documentId)
        }

    suspend fun createDocumentInternal(uri: Uri): Resource<Document> =
        wrapResponseIntoResource {
            documentRemoteSource.getDocumentFromUri(uri)
        }

    suspend fun createPartialDocument(documentData: ByteArray, contentType: String?,
                                      filename: String?, documentType: DocumentRemoteSource.Companion.DocumentType?,
                                      documentMetadata: DocumentMetadata?): Resource<Document> {
        return createPartialDocumentInternal(documentData, contentType, filename, documentType, documentMetadata)
    }

    suspend fun createPartialDocument(documentData: ByteArray, contentType: String?,
                                      filename: String?,
                                      documentType: DocumentRemoteSource.Companion.DocumentType?): Resource<Document> {
        return createPartialDocumentInternal(documentData, contentType, filename, documentType)
    }

    suspend fun createPartialDocumentInternal(documentData: ByteArray, contentType: String?,
                                              filename: String?, documentType: DocumentRemoteSource.Companion.DocumentType?,
                                              documentMetadata: DocumentMetadata? = null): Resource<Document> {
        var apiDoctypeHint: String? = null
        if (documentType != null) {
            apiDoctypeHint = documentType.apiDoctypeHint
        }

        val contentType = contentType?.let { MediaTypes.forPartialDocument(giniApiType.giniPartialMediaType, it) }
        val documentUri = documentRemoteSource.uploadDocument(documentData, contentType!!, filename, apiDoctypeHint, documentMetadata?.metadata)

        return createDocumentInternal(documentUri)
    }

    /**
     * Creates a new Gini composite document.
     *
     * @param documents    A list of partial documents which should be part of a multi-page document
     * @param documentType Optional a document type hint. See the documentation for the document type hints for
     *                     possible values
     * @return A Resource which has in the "data" the freshly created document.
     */

    suspend fun createCompositeDocument(documents: List<Document>, documentType: DocumentRemoteSource.Companion.DocumentType?): Resource<Document> {
        var apiDoctypeHint = documentType?.apiDoctypeHint
        val uriFromUpload = documentRemoteSource.uploadDocument(createCompositeJson(documents), giniApiType.giniCompositeJsonMediaType, null, apiDoctypeHint, null)
        return wrapResponseIntoResource {
            documentRemoteSource.getDocumentFromUri(uriFromUpload)
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

    suspend fun createCompositeDocument(documentRotationMap: LinkedHashMap<Document, Int>, documentType: DocumentRemoteSource.Companion.DocumentType?): Resource<Document> {
        val apiDoctypeHint = documentType?.apiDoctypeHint
        val uriFromUpload = documentRemoteSource.uploadDocument(createCompositeJson(documentRotationMap), giniApiType.giniCompositeJsonMediaType, null, apiDoctypeHint, null)
        return wrapResponseIntoResource {
            documentRemoteSource.getDocumentFromUri(uriFromUpload)
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

    suspend fun createDocument(document: ByteArray, filename: String?, documentType: DocumentRemoteSource.Companion.DocumentType?): Resource<Document> {
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

    suspend fun createDocument(document: ByteArray, filename: String?, documentType: DocumentRemoteSource.Companion.DocumentType?, documentMetadata: DocumentMetadata): Resource<Document> {
        return createDocumentInternal(document, filename, documentType, documentMetadata)
    }

    suspend fun createDocumentInternal(document: ByteArray, filename: String?, documentType: DocumentRemoteSource.Companion.DocumentType?, documentMetadata: DocumentMetadata?): Resource<Document> {
        var apiDoctypeHint: String? = null
        if (documentType != null) {
            apiDoctypeHint = documentType.apiDoctypeHint
        }

        val documentUri = documentRemoteSource.uploadDocument(document, MediaTypes.IMAGE_JPEG, filename, apiDoctypeHint, documentMetadata?.metadata)
        return createDocumentInternal(documentUri)
    }

    /**
     * Get the document with the given unique identifier.
     *
     * @param documentId The unique identifier of the document.
     * @return A Resource document instance representing all the document's metadata.
     */
    suspend fun getDocument(documentId: String): Resource<Document> =
        wrapResponseIntoResource {
            documentRemoteSource.getDocument(documentId)
        }

    @Throws(Exception::class)
    abstract fun createExtractionsContainer(specificExtractions: Map<String, SpecificExtraction>,
                                            compoundExtractions: Map<String, CompoundExtraction>,
                                            responseJSON: JSONObject): E

//    @Throws(JSONException::class)
//    fun parseSpecificExtractions(specificExtractionsJson: JSONObject, candidates: Map<String, List<Extraction>>): Map<String, SpecificExtraction> {
//        val specificExtractions = mapOf<String, SpecificExtraction>()
//        val extractionsNameIterator = specificExtractionsJson.keys()
//        while (extractionsNameIterator.hasNext()) {
//            val extractionName = extractionsNameIterator.next()
//            val extractionData = specificExtractionsJson.getJSONObject(extractionName)
//            val extraction: Extraction = extractionFromApiResponse(extractionData)
//            var candidatesForExtraction = listOf<Extraction>()
//            if (extractionData.has("candidates")) {
//                val candidatesName = extractionData.getString("candidates")
//                if (candidates.containsKey(candidatesName)) {
//                    candidatesForExtraction = candidates[candidatesName]
//                }
//            }
//            val specificExtraction = SpecificExtraction(
//                extractionName, extraction.value,
//                extraction.entity, extraction.box,
//                candidatesForExtraction
//            )
//            specificExtractions.put(extractionName, specificExtraction)
//        }
//
//        return specificExtractions
//    }

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
        const val POLLING_INTERVAL = 1000
        const val DEFAULT_COMPRESSION = 50

        private suspend fun <T> wrapResponseIntoResource(request: suspend () -> T) =
            try {
                Resource.Success(request())
            } catch (e: ApiException) {
                Resource.Error()
            } catch (e: CancellationException) {
                Resource.Cancelled()
            }
    }
}
