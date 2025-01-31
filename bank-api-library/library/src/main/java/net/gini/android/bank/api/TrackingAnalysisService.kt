package net.gini.android.bank.api

import net.gini.android.bank.api.requests.AmplitudeRequestBody
import net.gini.android.core.api.DocumentService
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface TrackingAnalysisService : DocumentService {

    @POST("/events/batch")
    @Headers(
        "Content-Type: application/vnd.gini.v1.events.amplitude",
        "Accept: application/json"
    )
    suspend fun sendEvents(
        @Header("Authorization") token: String,
        @Body amplitudeBody: AmplitudeRequestBody
    ): Response<ResponseBody>
}
