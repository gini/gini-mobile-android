package net.gini.android.health.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.response.PaymentRequestResponse
import net.gini.android.core.api.response.PaymentResponse
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.health.api.requests.PaymentRequestBody
import net.gini.android.health.api.response.AppVersionResponse
import net.gini.android.health.api.response.Colors
import net.gini.android.health.api.response.PageResponse
import net.gini.android.health.api.response.PaymentProviderResponse
import okhttp3.Headers
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.util.UUID

/**
 * Created by Alpár Szotyori on 21.07.23.
 *
 * Copyright (c) 2023 Gini GmbH.
 */

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HealthApiDocumentRemoteSourceTest {

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getPages`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            getPages(accessToken, "")
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getPaymentProviders`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            getPaymentProviders(accessToken)
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getPaymentProvider`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            getPaymentProvider(accessToken, "")
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in createPaymentRequest`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            createPaymentRequest(accessToken, PaymentRequestInput("", "", "", "", ""))
        }
    }

    private inline fun verifyAuthorizationHeader(
        expectedAuthorizationHeader: String,
        testScope: TestScope,
        testBlock: HealthApiDocumentRemoteSource.() -> Unit
    ) {
        // Given
        val documentServiceAuthInterceptor = DocumentServiceAuthInterceptor()
        val testSubject =
            HealthApiDocumentRemoteSource(
                StandardTestDispatcher(testScope.testScheduler),
                documentServiceAuthInterceptor,
                GiniHealthApiType(1),
                ""
            )

        // When
        with(testSubject) {
            testBlock()
        }
        testScope.advanceUntilIdle()

        // Then
        Truth.assertThat(documentServiceAuthInterceptor.bearerAuthHeader).isNotNull()
        Truth.assertThat(documentServiceAuthInterceptor.bearerAuthHeader).isEqualTo(expectedAuthorizationHeader)
    }

    private class DocumentServiceAuthInterceptor : HealthApiDocumentService {

        var bearerAuthHeader: String? = null

        override suspend fun getPages(bearer: Map<String, String>, documentId: String): Response<List<PageResponse>> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(listOf(PageResponse(0, emptyMap())))
        }

        override suspend fun getPaymentProviders(bearer: Map<String, String>): Response<List<PaymentProviderResponse>> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(
                listOf(
                    PaymentProviderResponse(
                        "",
                        "",
                        "",
                        AppVersionResponse(""),
                        Colors("", ""),
                        "",
                        "",
                        listOf(),
                        listOf()
                    )
                )
            )
        }

        override suspend fun getPaymentProvider(
            bearer: Map<String, String>,
            documentId: String
        ): Response<PaymentProviderResponse> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(PaymentProviderResponse("", "", "", AppVersionResponse(""), Colors("", ""), "", "", listOf("android"), listOf()))
        }

        override suspend fun createPaymentRequest(
            bearer: Map<String, String>,
            body: PaymentRequestBody
        ): Response<ResponseBody> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(null, Headers.Builder().set("Location", "somewhere").build())
        }

        override suspend fun getPayment(bearer: Map<String, String>, id: String): Response<PaymentResponse> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(PaymentResponse("", "", "", null, "", ""))
        }

        override suspend fun uploadDocument(
            bearer: Map<String, String>,
            bytes: RequestBody,
            fileName: String?,
            docType: String?
        ): Response<ResponseBody> {
            // Is tested in core api library
            return Response.success(null)
        }

        override suspend fun getDocument(bearer: Map<String, String>, documentId: String): Response<ResponseBody> {
            // Is tested in core api library
            return Response.success(null)
        }

        override suspend fun getDocumentFromUri(bearer: Map<String, String>, uri: String): Response<ResponseBody> {
            // Is tested in core api library
            return Response.success(null)
        }

        override suspend fun getExtractions(bearer: Map<String, String>, documentId: String): Response<ResponseBody> {
            // Is tested in core api library
            return Response.success(null)
        }

        override suspend fun deleteDocument(bearer: Map<String, String>, documentId: String): Response<ResponseBody> {
            // Is tested in core api library
            return Response.success(null)
        }

        override suspend fun deleteDocumentFromUri(
            bearer: Map<String, String>,
            documentUri: Uri
        ): Response<ResponseBody> {
            // Is tested in core api library
            return Response.success(null)
        }

        override suspend fun getLayoutForDocument(
            bearer: Map<String, String>,
            documentId: String
        ): Response<ResponseBody> {
            // Is tested in core api library
            return Response.success(null)
        }

        override suspend fun getPaymentRequest(
            bearer: Map<String, String>,
            id: String
        ): Response<PaymentRequestResponse> {
            // Is tested in core api library
            return Response.success(null)
        }

        override suspend fun getPaymentRequests(bearer: Map<String, String>): Response<List<PaymentRequestResponse>> {
            // Is tested in core api library
            return Response.success(null)
        }

        override suspend fun getFile(bearer: Map<String, String>, location: String): Response<ResponseBody> {
            // Is tested in core api library
            return Response.success(null)
        }

        override suspend fun sendFeedback(
            bearer: Map<String, String>,
            id: String,
            params: RequestBody
        ): Response<ResponseBody> {
            // Is tested in core api library
            return Response.success(null)
        }

        override suspend fun getPaymentRequestDocument(
            bearer: Map<String, String>,
            paymentRequestId: String
        ): Response<ResponseBody> {
            return Response.success(null)
        }

    }
}