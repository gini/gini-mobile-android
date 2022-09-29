package net.gini.android.bank.api

import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.bank.api.models.ReturnReason
import net.gini.android.core.api.DocumentRemoteSource
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.core.api.requests.ApiException
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

class BankApiDocumentRepository(
    override val coroutineContext: CoroutineContext,
    private val documentRemoteSource: BankApiDocumentRemoteSource,
    private val giniApiType: GiniBankApiType
) : DocumentRepository<ExtractionsContainer>(coroutineContext, documentRemoteSource, giniApiType) {

    override fun createExtractionsContainer(
        specificExtractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>,
        responseJSON: JSONObject
    ): ExtractionsContainer {
        val returnReasons: List<ReturnReason> = parseReturnReason(responseJSON.optJSONArray("returnReasons"))

        return ExtractionsContainer(specificExtractions, compoundExtractions, returnReasons)
    }

    suspend fun sendFeedback(documentId: String): Resource<ResponseBody> =
        wrapResponseIntoResource {
            documentRemoteSource.sendFeedback(documentId)
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