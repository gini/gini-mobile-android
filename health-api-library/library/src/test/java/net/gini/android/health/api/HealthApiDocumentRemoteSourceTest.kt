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

    // Note: Authentication is now handled by GiniAuthenticationInterceptor
    // These tests verify that RemoteSource methods work without manual token passing

    @Test
    fun `getPages works without manual auth`() = runTest {
        verifyMethodCall(this) {
            getPages("")
        }
    }

    @Test
    fun `getPaymentProviders works without manual auth`() = runTest {
        verifyMethodCall(this) {
            getPaymentProviders()
        }
    }

    @Test
    fun `getPaymentProvider works without manual auth`() = runTest {
        verifyMethodCall(this) {
            getPaymentProvider("")
        }
    }

    @Test
    fun `createPaymentRequest works without manual auth`() = runTest {
        verifyMethodCall(this) {
            createPaymentRequest(PaymentRequestInput("", "", "", "", ""))
        }
    }

    private inline fun verifyMethodCall(
        testScope: TestScope,
        testBlock: HealthApiDocumentRemoteSource.() -> Unit
    ) {
        // Given
        val mockService = MockHealthApiDocumentService()
        val testSubject =
            HealthApiDocumentRemoteSource(
                StandardTestDispatcher(testScope.testScheduler),
                mockService,
                GiniHealthApiType(1),
                ""
            )

        // When
        with(testSubject) {
            testBlock()
        }
        testScope.advanceUntilIdle()

        // Then - just verify no exception thrown
        // Authentication is tested in GiniAuthenticationInterceptorTest
    }

    private class MockHealthApiDocumentService : HealthApiDocumentService {

        override suspend fun getPages(documentId: String): Response<List<PageResponse>> {
            return Response.success(listOf(PageResponse(0, emptyMap())))
        }

        override suspend fun getPaymentProviders(): Response<List<PaymentProviderResponse>> {
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
            documentId: String
        ): Response<PaymentProviderResponse> {
            return Response.success(PaymentProviderResponse("", "", "", AppVersionResponse(""), Colors("", ""), "", "", listOf("android"), listOf()))
        }

        override suspend fun createPaymentRequest(
            body: PaymentRequestBody
        ): Response<ResponseBody> {
            return Response.success(null, Headers.Builder().set("Location", "somewhere").build())
        }

        override suspend fun getPayment(id: String): Response<PaymentResponse> {
            return Response.success(PaymentResponse("", "", "", null, "", ""))
        }

        override suspend fun uploadDocument(
            bytes: RequestBody,
            fileName: String?,
            docType: String?,
            headers: Map<String, String>
        ): Response<ResponseBody> {
            return Response.success(null)
        }

        override suspend fun getDocument(documentId: String): Response<ResponseBody> {
            return Response.success(null)
        }

        override suspend fun getDocumentFromUri(uri: String): Response<ResponseBody> {
            return Response.success(null)
        }

        override suspend fun getExtractions(documentId: String): Response<ResponseBody> {
            return Response.success(null)
        }

        override suspend fun deleteDocument(documentId: String): Response<ResponseBody> {
            return Response.success(null)
        }

        override suspend fun deleteDocumentFromUri(
            documentUri: Uri
        ): Response<ResponseBody> {
            return Response.success(null)
        }

        override suspend fun getPaymentRequest(
            id: String
        ): Response<PaymentRequestResponse> {
            return Response.success(null)
        }

        override suspend fun getPaymentRequests(): Response<List<PaymentRequestResponse>> {
            return Response.success(null)
        }

        override suspend fun getFile(location: String): Response<ResponseBody> {
            return Response.success(null)
        }

        override suspend fun sendFeedback(
            id: String,
            params: RequestBody
        ): Response<ResponseBody> {
            return Response.success(null)
        }

        override suspend fun getPaymentRequestDocument(
            paymentRequestId: String
        ): Response<ResponseBody> {
            return Response.success(null)
        }

        override suspend fun deletePaymentRequest(
            paymentRequestId: String
        ): Response<ResponseBody> {
            return Response.success(null)
        }

        override suspend fun getConfigurations(): Response<ConfigurationResponse> {
            return Response.success(null)
        }

        override suspend fun batchDeletePaymentRequests(
            body: List<String>
        ): Response<Unit> {
            return Response.success(null)
        }

        override suspend fun getDocumentLayout(
            documentId: String
        ): Response<DocumentLayoutResponse> {
            return Response.success(null)
        }

        override suspend fun getDocumentPages(
            documentId: String
        ): Response<List<DocumentPageResponse>> {
            return Response.success(null)
        }

        override suspend fun batchDeleteDocuments(
            body: List<String>
        ): Response<Void> {
            return Response.success(null)
        }

    }
}