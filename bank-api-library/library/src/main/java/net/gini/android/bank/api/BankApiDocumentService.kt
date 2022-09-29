package net.gini.android.bank.api

import android.graphics.Bitmap
import net.gini.android.bank.api.models.Payment
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.core.api.DocumentService
import net.gini.android.core.api.Resource
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface BankApiDocumentService: DocumentService {

    @POST("documents/{documentId}/extractions/feedback")
    suspend fun sendFeedback(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<ResponseBody>

    @GET("documents/{documentId}/pages/{pageNumber}/{dimension}")
    suspend fun getPreview(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String, @Path("pageNumber") pageNumber: Int, @Path("dimension") dimension: String): Response<Bitmap>

    @POST("paymentRequests/{id}/payment")
    suspend fun resolvePaymentRequests(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<ResolvedPayment>

    @GET("paymentRequests/{id}/payment")
    suspend fun getPayment(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<Payment>

    @POST("events/error")
    suspend fun logErrorEvent(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<ResponseBody>
}
