package net.gini.android.core.api

import android.net.Uri
import net.gini.android.core.api.response.DocumentLayoutResponse
import net.gini.android.core.api.response.DocumentPageResponse
import net.gini.android.core.api.response.PaymentRequestResponse
import net.gini.android.core.api.response.PaymentResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Internal use only.
 * 
 * Retrofit service interface for document-related API endpoints.
 * 
 * **Note**: Authentication is handled automatically by [GiniAuthenticationInterceptor].
 * No need to pass Bearer tokens manually via @HeaderMap.
 */
interface DocumentService {

    @POST("documents/")
    @Headers("Content-Type: application/octet-stream")
    suspend fun uploadDocument(
        @Body bytes: RequestBody,
        @Query("filename") fileName: String?,
        @Query("doctype") docType: String?,
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    @GET("documents/{documentId}")
    suspend fun getDocument(
        @Path("documentId") documentId: String
    ): Response<ResponseBody>

    @GET
    suspend fun getDocumentFromUri(
        @Url uri: String
    ): Response<ResponseBody>

    @GET("documents/{documentId}/extractions")
    suspend fun getExtractions(
        @Path("documentId") documentId: String
    ): Response<ResponseBody>

    suspend fun sendFeedback(id: String, params: RequestBody): Response<ResponseBody> {
        throw NotImplementedError("sendFeedback must be overridden by interfaces extending DocumentService")
    }

    @DELETE("documents/{documentId}")
    suspend fun deleteDocument(
        @Path("documentId") documentId: String
    ): Response<ResponseBody>

    @DELETE
    suspend fun deleteDocumentFromUri(
        @Url documentUri: Uri
    ): Response<ResponseBody>

    @GET("documents/{documentId}/layout")
    suspend fun getDocumentLayout(
        @Path("documentId") documentId: String
    ): Response<DocumentLayoutResponse>

    @GET("documents/{documentId}/pages")
    suspend fun getDocumentPages(
        @Path("documentId") documentId: String
    ): Response<List<DocumentPageResponse>>

    @GET("paymentRequests/{id}")
    suspend fun getPaymentRequest(
        @Path("id") id: String
    ): Response<PaymentRequestResponse>

    @GET("paymentRequests")
    suspend fun getPaymentRequests(): Response<List<PaymentRequestResponse>>

    @GET("paymentRequests/{id}/payment")
    suspend fun getPayment(
        @Path("id") id: String
    ): Response<PaymentResponse>

    @GET
    suspend fun getFile(
        @Url location: String
    ): Response<ResponseBody>
}
