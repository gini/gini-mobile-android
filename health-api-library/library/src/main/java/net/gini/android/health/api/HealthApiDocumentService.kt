package net.gini.android.health.api

import net.gini.android.core.api.DocumentService
import net.gini.android.health.api.requests.PaymentRequestBody
import net.gini.android.health.api.response.PageResponse
import net.gini.android.health.api.response.PaymentProviderResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Path

internal interface HealthApiDocumentService: DocumentService {

    @POST("documents/{documentId}/extractions")
    override suspend fun sendFeedback(@HeaderMap bearer: Map<String, String>, @Path("documentId") id: String, @Body params: RequestBody): Response<ResponseBody>

    @GET("/documents/{documentId}/pages")
    suspend fun getPages(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Response<List<PageResponse>>

    @GET("/paymentProviders")
    suspend fun getPaymentProviders(@HeaderMap bearer: Map<String, String>): Response<List<PaymentProviderResponse>>

    @GET("/paymentProviders/{providerId}")
    suspend fun getPaymentProvider(@HeaderMap bearer: Map<String, String>, @Path("providerId") documentId: String): Response<PaymentProviderResponse>

    @POST("/paymentRequests")
    suspend fun createPaymentRequest(@HeaderMap bearer: Map<String, String>, @Body body: PaymentRequestBody): Response<ResponseBody>
}