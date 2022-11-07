package net.gini.android.core.api

import android.net.Uri
import net.gini.android.core.api.authorization.UserService
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.response.PaymentRequestResponse
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

    @DELETE("documents/{documentId}")
    suspend fun deleteDocument(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<ResponseBody>

    @DELETE
    suspend fun deleteDocumentFromUri(@HeaderMap bearer: Map<String, String>, @Url documentUri: Uri): Response<ResponseBody>

    @GET("documents/{documentId}/layout")
    suspend fun getLayoutForDocument(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<ResponseBody>

    @GET("paymentRequests/{id}")
    suspend fun getPaymentRequest(@HeaderMap bearer: Map<String, String>, @Path("id") id: String): Response<PaymentRequestResponse>

    @GET("paymentRequests")
    suspend fun getPaymentRequests(@HeaderMap bearer: Map<String, String>): Response<List<PaymentRequestResponse>>

    @GET
    suspend fun getFile(@HeaderMap bearer: Map<String, String>, @Url location:String): Response<ResponseBody>
}
