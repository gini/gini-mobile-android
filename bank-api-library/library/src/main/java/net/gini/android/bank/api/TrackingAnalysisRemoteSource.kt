package net.gini.android.bank.api

import kotlinx.coroutines.withContext
import net.gini.android.bank.api.models.AmplitudeRoot
import net.gini.android.bank.api.requests.toAmplitudeRequestBody
import net.gini.android.core.api.requests.SafeApiRequest
import kotlin.coroutines.CoroutineContext

/**
 * Internal use only.
 */
class TrackingAnalysisRemoteSource internal constructor(
    private val coroutineContext: CoroutineContext,
    private val trackingAnalysisService: TrackingAnalysisService
) {

    suspend fun sendEvents(amplitudeRoot: AmplitudeRoot): Unit =
        withContext(coroutineContext) {
            SafeApiRequest.apiRequest {
                trackingAnalysisService.sendEvents(amplitudeRoot.toAmplitudeRequestBody())
            }
        }

}
