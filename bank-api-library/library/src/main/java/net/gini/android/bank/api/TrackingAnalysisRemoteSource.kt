package net.gini.android.bank.api

import kotlinx.coroutines.withContext
import net.gini.android.bank.api.models.AmplitudeRoot
import net.gini.android.bank.api.requests.toAmplitudeRequestBody
import net.gini.android.core.api.requests.BearerAuthorizatonHeader
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
                trackingAnalysisService.sendEvents(
                    // The Authorization header is added by the SDK's session interceptor
                    token = null,
                    amplitudeBody = amplitudeRoot.toAmplitudeRequestBody()
                )
            }
        }

    @Deprecated(
        "The Authorization header is added by the SDK's session interceptor in the OkHttp layer. " +
                "Use the overload without an accessToken parameter."
    )
    suspend fun sendEvents(accessToken: String, amplitudeRoot: AmplitudeRoot): Unit =
        withContext(coroutineContext) {
            SafeApiRequest.apiRequest {
                trackingAnalysisService.sendEvents(
                    BearerAuthorizatonHeader(accessToken).value,
                    amplitudeRoot.toAmplitudeRequestBody()
                )
            }
        }

}
