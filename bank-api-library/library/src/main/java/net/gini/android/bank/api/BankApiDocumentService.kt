package net.gini.android.bank.api

import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.bank.api.requests.ResolvePaymentBody
import net.gini.android.bank.api.response.ConfigurationResponse
import net.gini.android.bank.api.response.ResolvePaymentResponse
import net.gini.android.core.api.DocumentService
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Path

internal interface BankApiDocumentService : DocumentService {

    @POST("documents/{id}/extractions/feedback")
    override suspend fun sendFeedback(
        @HeaderMap bearer: Map<String, String>, @Path("id") id: String, @Body params: RequestBody
    )
            : Response<ResponseBody>

    @POST("paymentRequests/{id}/payment")
    suspend fun resolvePaymentRequests(
        @HeaderMap bearer: Map<String, String>,
        @Path("id") id: String,
        @Body input: ResolvePaymentBody
    ): Response<ResolvePaymentResponse>

    @POST("events/error")
    suspend fun logErrorEvent(
        @HeaderMap bearer: Map<String, String>,
        @Body errorEvent: ErrorEvent
    ): Response<ResponseBody>

    @GET("configurations")
    suspend fun getConfigurations(@HeaderMap bearer: Map<String, String>)
            : Response<ConfigurationResponse>
}
