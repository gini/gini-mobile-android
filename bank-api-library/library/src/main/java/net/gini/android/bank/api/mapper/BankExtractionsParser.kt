package net.gini.android.bank.api.mapper

import net.gini.android.bank.api.models.ReturnReason
import org.json.JSONArray
import org.json.JSONException

/**
 * Internal use only.
 *
 * Parses the Bank API specific return reasons of the extractions response JSON.
 */
internal object BankExtractionsParser {

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
