package net.gini.android.bank.api.mapper

import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.bank.api.models.ReturnReason
import net.gini.android.core.api.mapper.ExtractionsParser
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Parses the Gini Bank API extractions response JSON into the Bank SDK's extraction models,
 * including the Bank API specific return reasons.
 *
 * Use this when you do the API calls yourself (with your own network implementation) and want
 * to map the extractions response body to the same models the SDK produces.
 */
object BankExtractionsParser {

    /**
     * Parses a complete extractions response (the response body of
     * `GET /documents/{id}/extractions`) into an [ExtractionsContainer] with specific
     * extractions, compound extractions and return reasons.
     *
     * @param extractionsResponse the extractions response JSON
     * @return The parsed [ExtractionsContainer].
     * @throws JSONException If the JSON does not have the expected structure or contains invalid data.
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun parseExtractionsContainer(extractionsResponse: JSONObject): ExtractionsContainer {
        val candidates = ExtractionsParser.parseCandidates(extractionsResponse.getJSONObject("candidates"))
        val specificExtractions = ExtractionsParser.parseSpecificExtractions(
            extractionsResponse.getJSONObject("extractions"),
            candidates
        )
        val compoundExtractions = ExtractionsParser.parseCompoundExtractions(
            extractionsResponse.optJSONObject("compoundExtractions"),
            candidates
        )
        val returnReasons = parseReturnReasons(extractionsResponse.optJSONArray("returnReasons"))
        return ExtractionsContainer(specificExtractions, compoundExtractions, returnReasons)
    }

    /**
     * Parses the `returnReasons` array of the extractions response JSON.
     *
     * @param returnReasonsJson the value of the `returnReasons` key (may be null)
     * @return The parsed [ReturnReason]s or an empty list when the JSON array is null.
     * @throws JSONException If the JSON does not have the expected structure or contains invalid data.
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun parseReturnReasons(returnReasonsJson: JSONArray?): List<ReturnReason> {
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
