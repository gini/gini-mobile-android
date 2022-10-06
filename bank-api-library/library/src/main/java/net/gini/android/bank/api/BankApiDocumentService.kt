package net.gini.android.bank.api

import android.graphics.Bitmap
import net.gini.android.bank.api.models.FeedbackRequestModel
import net.gini.android.bank.api.models.Payment
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.core.api.DocumentService
import net.gini.android.core.api.Resource
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface BankApiDocumentService: DocumentService {

    @POST("documents/{documentId}/extractions/feedback")
    suspend fun sendFeedback(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String, @Body params: RequestBody): Response<ResponseBody>

    @POST("paymentRequests/{id}/payment")
    suspend fun resolvePaymentRequests(@HeaderMap bearer: Map<String, String>, @Path("id") id: String, @Body input: ResolvePaymentInput): Response<ResolvedPayment>

    @GET("paymentRequests/{id}/payment")
    suspend fun getPayment(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<Payment>

    @POST("events/error")
    suspend fun logErrorEvent(@HeaderMap bearer: Map<String, String>, @Body errorEvent: ErrorEvent): Response<ResponseBody>
}
