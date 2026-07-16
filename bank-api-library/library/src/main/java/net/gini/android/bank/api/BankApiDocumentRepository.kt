package net.gini.android.bank.api

import net.gini.android.bank.api.mapper.BankExtractionsParser
import net.gini.android.bank.api.models.AmplitudeRoot
import net.gini.android.bank.api.models.Configuration
import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.bank.api.models.ReturnReason
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.Resource
import net.gini.android.core.api.Resource.Companion.wrapInResource
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.SpecificExtraction
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Internal use only.
 */
class BankApiDocumentRepository(
    private val documentRemoteSource: BankApiDocumentRemoteSource,
    sessionManager: SessionManager,
    giniApiType: GiniBankApiType,
    private val trackingAnalysisRemoteSource: TrackingAnalysisRemoteSource
) : DocumentRepository<ExtractionsContainer>(documentRemoteSource, sessionManager, giniApiType) {

    override fun createExtractionsContainer(
        specificExtractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>,
        responseJSON: JSONObject
    ): ExtractionsContainer {
        val returnReasons: List<ReturnReason> =
            BankExtractionsParser.parseReturnReasons(responseJSON.optJSONArray("returnReasons"))

        return ExtractionsContainer(specificExtractions, compoundExtractions, returnReasons)
    }

    suspend fun resolvePaymentRequest(
        requestId: String,
        resolvePaymentInput: ResolvePaymentInput
    ): Resource<ResolvedPayment> =
        wrapInResource {
            documentRemoteSource.resolvePaymentRequests(
                requestId,
                resolvePaymentInput
            )
        }

    suspend fun logErrorEvent(errorEvent: ErrorEvent): Resource<Unit> =
        wrapInResource {
            documentRemoteSource.logErrorEvent(errorEvent)
        }

    suspend fun getConfigurations(): Resource<Configuration> =
        wrapInResource {
            documentRemoteSource.getConfigurations()
        }

    suspend fun sendEvents(amplitudeRoot: AmplitudeRoot): Resource<Unit> =
        wrapInResource {
            trackingAnalysisRemoteSource.sendEvents(amplitudeRoot)
        }
}
