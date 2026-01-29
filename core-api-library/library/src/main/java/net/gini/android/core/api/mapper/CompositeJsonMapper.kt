package net.gini.android.core.api.mapper

import net.gini.android.core.api.Utils
import net.gini.android.core.api.models.Document
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


@Throws(JSONException::class)
fun createCompositeJson(documents: List<Document>): ByteArray {
    val documentRotationMap = linkedMapOf<Document, Int>()
    for (document in documents) {
        documentRotationMap[document] = 0
    }

    return createCompositeJson(documentRotationMap)
}

@Throws(JSONException::class)
fun createCompositeJson(documentRotationMap: LinkedHashMap<Document, Int>): ByteArray {
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