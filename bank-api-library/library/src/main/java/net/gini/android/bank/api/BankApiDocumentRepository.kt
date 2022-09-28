package net.gini.android.bank.api

import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.bank.api.models.ReturnReason
import net.gini.android.core.api.DocumentRemoteSource
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.SpecificExtraction
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext

class BankApiDocumentRepository(
    override val coroutineContext: CoroutineContext,
    documentRemoteSource: DocumentRemoteSource,
    giniApiType: GiniApiType
) : DocumentRepository<ExtractionsContainer>(coroutineContext, documentRemoteSource, giniApiType) {

    override fun createExtractionsContainer(
        specificExtractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>,
        responseJSON: JSONObject
    ): ExtractionsContainer {
        val returnReasons: List<ReturnReason> = parseReturnReason(responseJSON.optJSONArray("returnReasons"))

        return ExtractionsContainer(specificExtractions, compoundExtractions, returnReasons)
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