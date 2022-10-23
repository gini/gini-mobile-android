package net.gini.android.bank.api

import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.bank.api.requests.ResolvePaymentBody
import net.gini.android.bank.api.response.PaymentResponse
import net.gini.android.bank.api.response.ResolvePaymentResponse
import net.gini.android.core.api.DocumentService
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

internal interface BankApiDocumentService: DocumentService {

    @POST("documents/{id}/extractions/feedback")
    suspend fun sendFeedback(@HeaderMap bearer: Map<String, String>, @Path("id") id: String, @Body params: RequestBody): Response<ResponseBody>

    @POST("paymentRequests/{id}/payment")
    suspend fun resolvePaymentRequests(@HeaderMap bearer: Map<String, String>, @Path("id") id: String, @Body input: ResolvePaymentBody): Response<ResolvePaymentResponse>

    @GET("paymentRequests/{id}/payment")
    suspend fun getPayment(@HeaderMap bearer: Map<String, String>, @Path("id") id: String): Response<PaymentResponse>

    @POST("events/error")
    suspend fun logErrorEvent(@HeaderMap bearer: Map<String, String>, @Body errorEvent: ErrorEvent): Response<ResponseBody>
}
