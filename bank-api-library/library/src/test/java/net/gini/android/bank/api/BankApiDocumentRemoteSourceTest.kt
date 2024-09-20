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

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in resolvePaymentRequests`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            resolvePaymentRequests(accessToken, "", ResolvePaymentInput("", "", "", ""))
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getPayment`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            getPayment(accessToken, "")
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in logErrorEvent`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            logErrorEvent(accessToken, ErrorEvent("", "", "", "", "", "", ""))
        }
    }

    private inline fun verifyAuthorizationHeader(
        expectedAuthorizationHeader: String,
        testScope: TestScope,
        testBlock: BankApiDocumentRemoteSource.() -> Unit
    ) {
        // Given
        val documentServiceAuthInterceptor = DocumentServiceAuthInterceptor()
        val testSubject =
            BankApiDocumentRemoteSource(
                StandardTestDispatcher(testScope.testScheduler),
                documentServiceAuthInterceptor,
                GiniBankApiType(1),
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

    private class DocumentServiceAuthInterceptor : BankApiDocumentService {

        var bearerAuthHeader: String? = null

        override suspend fun resolvePaymentRequests(
            bearer: Map<String, String>,
            id: String,
            input: ResolvePaymentBody
        ): Response<ResolvePaymentResponse> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(ResolvePaymentResponse("", "", "", null, "", "", ""))
        }

        override suspend fun getPayment(bearer: Map<String, String>, id: String): Response<PaymentResponse> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(PaymentResponse("", "", "", null, "", ""))
        }

        override suspend fun logErrorEvent(
            bearer: Map<String, String>,
            errorEvent: ErrorEvent
        ): Response<ResponseBody> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(null)
        }

        override suspend fun getConfigurations(bearer: Map<String, String>): Response<ConfigurationResponse> {
            return Response.success(ConfigurationResponse(null, null, null, null, null, null))
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

    }
}