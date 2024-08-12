package net.gini.android.bank.api

import net.gini.android.bank.api.requests.AmplitudeRequestBody
import net.gini.android.core.api.DocumentService
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface TrackingAnalysisService: DocumentService {

    @POST("/batch")
    suspend fun sendEvents(@Body amplitudeBody: AmplitudeRequestBody): Response<ResponseBody>
}
