package net.gini.android.core.api

import android.net.Uri
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.response.PaymentRequestResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface DocumentService {

    @POST("documents/")
    suspend fun uploadDocument(@HeaderMap bearer: Map<String, String>, @Body bytes: ByteArray,  @Query("filename") fileName: String?, @Query("doctype") docType: String?): Response<ResponseBody>

    @GET("documents/{documentId}")
    suspend fun getDocument(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<Document>

    @GET
    suspend fun getDocumentFromUri(@HeaderMap bearer: Map<String, String>, @Url uri:String): Response<Document>

    @GET("documents/{documentId}/extractions")
    suspend fun getExtractions(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<ResponseBody>

    @GET("documents/{documentId}/extractions")
    suspend fun getIncubatorExtractions(@HeaderMap incubatorHeader: Map<String, String>, @Path("documentId") documentId: String): Response<ResponseBody>

    @DELETE("documents/{documentId}")
    suspend fun deleteDocument(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<String>

    @DELETE
    suspend fun deleteDocumentFromUri(@HeaderMap bearer: Map<String, String>, @Url documentUri: Uri): Response<String>

    @POST("documents/{documentId}/errorreport")
    suspend fun errorReportForDocument(@HeaderMap bearer: Map<String, String>, @Query("summary") summary: String, @Query("description") description: String): Response<ResponseBody>

    @GET("documents/{documentId}/layout")
    suspend fun getLayoutForDocument(@HeaderMap bearer: Map<String, String>): Response<ResponseBody>

    @GET("documents")
    suspend fun getDocumentList(@HeaderMap bearer: Map<String, String>, @Query("offset") offset: String, @Query("limit") limit: String): Response<ResponseBody>

    @GET("search")
    suspend fun searchDocument(@HeaderMap bearer: Map<String, String>, @Query("q") searchTerm: String, @Query("offset") offset: String, @Query("limit") limit: String, @Query("docType") docType: String): Response<ResponseBody>

    @GET("paymentRequests/{id}")
    suspend fun getPaymentRequest(@HeaderMap bearer: Map<String, String>): Response<PaymentRequestResponse>

    @GET("paymentRequests")
    suspend fun getPaymentRequests(@HeaderMap bearer: Map<String, String>): Response<List<PaymentRequestResponse>>

    @GET
    @Streaming
    suspend fun getFile(@HeaderMap bearer: Map<String, String>, @Url location:String): Response<ByteArray>
}
