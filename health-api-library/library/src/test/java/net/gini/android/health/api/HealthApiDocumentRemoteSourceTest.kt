package net.gini.android.health.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.response.DocumentLayoutResponse
import net.gini.android.core.api.response.DocumentPageResponse
import net.gini.android.core.api.response.PaymentRequestResponse
import net.gini.android.core.api.response.PaymentResponse
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.health.api.requests.PaymentRequestBody
import net.gini.android.health.api.response.AppVersionResponse
import net.gini.android.health.api.response.Colors
import net.gini.android.health.api.response.ConfigurationResponse
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

    // The Authorization header is added by the SDK's session interceptor in the OkHttp layer
    // (PP-2363). The remote source must not add it so that the interceptor (or a consumer
    // managing authentication themselves) stays in control of it.

    @Test
    fun `does not set authorization header in getPages`() = runTest {
        verifyNoAuthorizationHeader(this) {
            getPages("")
        }
    }

    @Test
    fun `does not set authorization header in getPaymentProviders`() = runTest {
        verifyNoAuthorizationHeader(this) {
            getPaymentProviders()
        }
    }

    @Test
    fun `does not set authorization header in getPaymentProvider`() = runTest {
        verifyNoAuthorizationHeader(this) {
            getPaymentProvider("")
        }
    }

    @Test
    fun `does not set authorization header in createPaymentRequest`() = runTest {
        verifyNoAuthorizationHeader(this) {
            createPaymentRequest(PaymentRequestInput("", "", "", "", ""))
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated createPaymentRequest with access token still sets bearer authorization header`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        val documentServiceAuthInterceptor = DocumentServiceAuthInterceptor()
        val testSubject = HealthApiDocumentRemoteSource(
            StandardTestDispatcher(testScheduler),
            documentServiceAuthInterceptor,
            GiniHealthApiType(1),
            ""
        )

        testSubject.createPaymentRequest(accessToken, PaymentRequestInput("", "", "", "", ""))
        advanceUntilIdle()

        Truth.assertThat(documentServiceAuthInterceptor.bearerAuthHeader).isNotNull()
        Truth.assertThat(documentServiceAuthInterceptor.bearerAuthHeader).isEqualTo(expectedAuthorizationHeader)
    }

    @Test
    fun `keeps absolute sourceDocumentLocation unchanged when it is not null`() = runTest {
        // Given
        val documentServiceAuthInterceptor = DocumentServiceAuthInterceptor()

        val baseUrl = "https://health.gini.net/"
        val apiType = GiniHealthApiType(
            apiVersion = 1,
            baseUrl = baseUrl
        )
        val testSubject = HealthApiDocumentRemoteSource(
            StandardTestDispatcher(testScheduler),
            documentServiceAuthInterceptor,
            // Make sure this type uses your baseUrl (depends on your actual implementation)
            apiType,
            ""
        )

        val input = PaymentRequestInput(
            sourceDocumentLocation = "https://health-api.gini.net/documents/065da89f-2f3c-45dc-a6da-cc90b5e8c242",
            paymentProvider = "pp",
            recipient = "recipient",
            iban = "iban",
            amount = "10:EUR",
            bic = null,
            purpose = "purpose"
        )

        // When
        testSubject.createPaymentRequest(input)
        advanceUntilIdle()

        // Then
        val body = documentServiceAuthInterceptor.lastPaymentRequestBody
        Truth.assertThat(body).isNotNull()
        Truth.assertThat(body!!.sourceDocumentLocation)
            .isEqualTo("https://health-api.gini.net/documents/065da89f-2f3c-45dc-a6da-cc90b5e8c242")
    }

    @Test
    fun `does not set sourceDocumentLocation when it is null`() = runTest {
        // Given
        val documentServiceAuthInterceptor = DocumentServiceAuthInterceptor()

        val baseUrl = "https://health.gini.net/"
        val apiType = GiniHealthApiType(
            apiVersion = 1,
            baseUrl = baseUrl
        )

        val testSubject = HealthApiDocumentRemoteSource(
            StandardTestDispatcher(testScheduler),
            documentServiceAuthInterceptor,
            apiType,
            ""
        )

        val input = PaymentRequestInput(
            sourceDocumentLocation = null,
            paymentProvider = "pp",
            recipient = "recipient",
            iban = "iban",
            amount = "10:EUR",
            bic = null,
            purpose = "purpose"
        )

        // When
        testSubject.createPaymentRequest(input)
        advanceUntilIdle()

        // Then
        val body = documentServiceAuthInterceptor.lastPaymentRequestBody
        Truth.assertThat(body).isNotNull()
        Truth.assertThat(body!!.sourceDocumentLocation).isNull()
    }

    private inline fun verifyNoAuthorizationHeader(
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
        Truth.assertThat(documentServiceAuthInterceptor.capturedHeaders).isNotNull()
        Truth.assertThat(documentServiceAuthInterceptor.capturedHeaders).doesNotContainKey("Authorization")
        Truth.assertThat(documentServiceAuthInterceptor.capturedHeaders).containsKey("Accept")
    }

    private class DocumentServiceAuthInterceptor : HealthApiDocumentService {

        var bearerAuthHeader: String? = null
        var capturedHeaders: Map<String, String>? = null
        var lastPaymentRequestBody: PaymentRequestBody? = null


        override suspend fun getPages(bearer: Map<String, String>, documentId: String): Response<List<PageResponse>> {
            bearerAuthHeader = bearer["Authorization"]
            capturedHeaders = bearer
            return Response.success(listOf(PageResponse(0, emptyMap())))
        }

        override suspend fun getPaymentProviders(bearer: Map<String, String>): Response<List<PaymentProviderResponse>> {
            bearerAuthHeader = bearer["Authorization"]
            capturedHeaders = bearer
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
            capturedHeaders = bearer
            return Response.success(PaymentProviderResponse("", "", "", AppVersionResponse(""), Colors("", ""), "", "", listOf("android"), listOf()))
        }

        override suspend fun createPaymentRequest(
            bearer: Map<String, String>,
            body: PaymentRequestBody
        ): Response<ResponseBody> {
            bearerAuthHeader = bearer["Authorization"]
            capturedHeaders = bearer
            lastPaymentRequestBody = body
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

        override suspend fun deletePaymentRequest(
            bearer: Map<String, String>,
            paymentRequestId: String
        ): Response<ResponseBody> {
            return Response.success(null)
        }

        override suspend fun getConfigurations(bearer: Map<String, String>): Response<ConfigurationResponse> {
            return Response.success(null)
        }

        override suspend fun batchDeletePaymentRequests(
            bearer: Map<String, String>,
            body: List<String>
        ): Response<Unit> {
            return Response.success(null)
        }

        override suspend fun getDocumentLayout(
            bearer: Map<String, String>,
            documentId: String
        ): Response<DocumentLayoutResponse> {
            return Response.success(null)
        }

        override suspend fun getDocumentPages(
            bearer: Map<String, String>,
            documentId: String
        ): Response<List<DocumentPageResponse>> {
            return Response.success(null)
        }

        override suspend fun batchDeleteDocuments(
            bearer: Map<String, String>,
            body: List<String>
        ): Response<Void> {
            return Response.success(null)
        }

    }
}
