package net.gini.android.core.api.mapper

import net.gini.android.core.api.Utils
import net.gini.android.core.api.models.Document
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Creates the request body JSON for creating composite documents from partial documents
 * (`POST /documents/` with the `application/vnd.gini.vX.composite+json` content type).
 */
internal object CompositeDocumentJson {

    /**
     * Creates the composite document request body for the given partial documents with a
     * rotation of 0 degrees for each.
     *
     * @param documents the partial documents in the order of the pages
     * @return The request body as a UTF-8 encoded JSON byte array.
     * @throws JSONException If the JSON could not be created.
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun create(documents: List<Document>): ByteArray {
        val documentRotationMap = linkedMapOf<Document, Int>()
        for (document in documents) {
            documentRotationMap[document] = 0
        }

        return create(documentRotationMap)
    }

    /**
     * Creates the composite document request body for the given partial documents and their
     * rotations. Rotations are normalized to 0-359 degrees.
     *
     * @param documentRotationMap mapping from partial documents to their rotation in degrees,
     * in the order of the pages
     * @return The request body as a UTF-8 encoded JSON byte array.
     * @throws JSONException If the JSON could not be created.
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun create(documentRotationMap: LinkedHashMap<Document, Int>): ByteArray {
        val jsonObject = JSONObject()
        val partialDocuments = JSONArray()

        for (entry in documentRotationMap.entries) {
            val document = entry.key
            var rotation = entry.value

            rotation = ((rotation % ROTATION_FULL_CIRCLE) + ROTATION_FULL_CIRCLE) % ROTATION_FULL_CIRCLE
            val partialDoc = JSONObject()
            partialDoc.put("document", document.uri)
            partialDoc.put("rotationDelta", rotation)
            partialDocuments.put(partialDoc)
        }

        jsonObject.put("partialDocuments", partialDocuments)

        return jsonObject.toString().toByteArray(Utils.CHARSET_UTF8)
    }

    private const val ROTATION_FULL_CIRCLE = 360
}
