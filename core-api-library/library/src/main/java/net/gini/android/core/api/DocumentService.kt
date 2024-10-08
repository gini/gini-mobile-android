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
 */
interface DocumentService {

    @POST("documents/")
    suspend fun uploadDocument(@HeaderMap bearer: Map<String, String>, @Body bytes: RequestBody, @Query("filename") fileName: String?, @Query("doctype") docType: String?): Response<ResponseBody>

    @GET("documents/{documentId}")
    suspend fun getDocument(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<ResponseBody>

    @GET
    suspend fun getDocumentFromUri(@HeaderMap bearer: Map<String, String>, @Url uri:String): Response<ResponseBody>

    @GET("documents/{documentId}/extractions")
    suspend fun getExtractions(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<ResponseBody>

    suspend fun sendFeedback(bearer: Map<String, String>, id: String, params: RequestBody): Response<ResponseBody> {
        throw NotImplementedError("sendFeedback must be overridden by interfaces extending DocumentService")
    }

    @DELETE("documents/{documentId}")
    suspend fun deleteDocument(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<ResponseBody>

    @DELETE
    suspend fun deleteDocumentFromUri(@HeaderMap bearer: Map<String, String>, @Url documentUri: Uri): Response<ResponseBody>

    @Deprecated(
        "This function is deprecated. Use another one, please.",
        replaceWith = ReplaceWith("getLayoutModel(documentId)"))
    @GET("documents/{documentId}/layout")
    suspend fun getLayoutForDocument(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<ResponseBody>

    @GET("documents/{documentId}/layout")
    suspend fun getDocumentLayout(
        @HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String
    ) : Response<DocumentLayoutResponse>

    @GET("documents/{documentId}/pages")
    suspend fun getDocumentPages(
        @HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String
    ) : Response<List<DocumentPageResponse>>

    @GET("paymentRequests/{id}")
    suspend fun getPaymentRequest(@HeaderMap bearer: Map<String, String>, @Path("id") id: String): Response<PaymentRequestResponse>

    @GET("paymentRequests")
    suspend fun getPaymentRequests(@HeaderMap bearer: Map<String, String>): Response<List<PaymentRequestResponse>>

    @GET("paymentRequests/{id}/payment")
    suspend fun getPayment(@HeaderMap bearer: Map<String, String>, @Path("id") id: String): Response<PaymentResponse>

    @GET
    suspend fun getFile(@HeaderMap bearer: Map<String, String>, @Url location:String): Response<ResponseBody>
}
