package net.gini.android.bank.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.bank.api.requests.ResolvePaymentBody
import net.gini.android.bank.api.response.ConfigurationResponse
import net.gini.android.bank.api.response.ResolvePaymentResponse
import net.gini.android.core.api.response.DocumentLayoutResponse
import net.gini.android.core.api.response.DocumentPageResponse
import net.gini.android.core.api.response.PaymentRequestResponse
import net.gini.android.core.api.response.PaymentResponse
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
class BankApiDocumentRemoteSourceTest {

    // Note: Authentication is now handled by GiniAuthenticationInterceptor
    // These tests verify that RemoteSource methods work without manual token passing

    @Test
    fun `resolvePaymentRequests works without manual auth`() = runTest {
        verifyMethodCall(this) {
            resolvePaymentRequests("", ResolvePaymentInput("", "", "", ""))
        }
    }

    @Test
    fun `getPayment works without manual auth`() = runTest {
        verifyMethodCall(this) {
            getPayment("")
        }
    }

    @Test
    fun `logErrorEvent works without manual auth`() = runTest {
        verifyMethodCall(this) {
            logErrorEvent(ErrorEvent("", "", "", "", "", "", ""))
        }
    }

    private inline fun verifyMethodCall(
        testScope: TestScope,
        testBlock: BankApiDocumentRemoteSource.() -> Unit
    ) {
        // Given
        val mockService = MockBankApiDocumentService()
        val testSubject =
            BankApiDocumentRemoteSource(
                StandardTestDispatcher(testScope.testScheduler),
                mockService,
                GiniBankApiType(1),
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

    private class MockBankApiDocumentService : BankApiDocumentService {

        override suspend fun resolvePaymentRequests(
            id: String,
            input: ResolvePaymentBody
        ): Response<ResolvePaymentResponse> {
            return Response.success(ResolvePaymentResponse("", "", "", null, "", "", ""))
        }

        override suspend fun getPayment(id: String): Response<PaymentResponse> {
            return Response.success(PaymentResponse("", "", "", null, "", ""))
        }

        override suspend fun logErrorEvent(
            errorEvent: ErrorEvent
        ): Response<ResponseBody> {
            return Response.success(null)
        }

        override suspend fun getConfigurations(): Response<ConfigurationResponse> {
            return Response.success(
                ConfigurationResponse(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            )
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

    }
}