package net.gini.android.core.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.response.DocumentLayoutResponse
import net.gini.android.core.api.response.DocumentPageResponse
import net.gini.android.core.api.response.PaymentRequestResponse
import net.gini.android.core.api.response.PaymentResponse
import net.gini.android.core.api.test.DocumentRemoteSourceForTests
import net.gini.android.core.api.test.MockGiniApiType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import com.google.common.truth.Truth

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DocumentRemoteSourceTest {

    // Note: Authentication is now handled by GiniAuthenticationInterceptor
    // These tests verify that methods can be called without manual token passing

    @Test
    fun `deleteDocument with document id works without manual auth`() = runTest {
        verifyMethodCall(this) {
            deleteDocument("")
        }
    }

    @Test
    fun `deleteDocument with uri works without manual auth`() = runTest {
        verifyMethodCall(this) {
            deleteDocument(Uri.EMPTY)
        }
    }

    @Test
    fun `getDocument works without manual auth`() = runTest {
        verifyMethodCall(this) {
            getDocument("")
        }
    }

    @Test
    fun `getDocumentFromUri works without manual auth`() = runTest {
        verifyMethodCall(this) {
            getDocumentFromUri(Uri.EMPTY)
        }
    }

    @Test
    fun `getExtractions works without manual auth`() = runTest {
        verifyMethodCall(this) {
            getExtractions("")
        }
    }

    @Test
    fun `getFile works without manual auth`() = runTest {
        verifyMethodCall(this) {
            getFile("")
        }
    }

    @Test
    fun `getDocumentLayout works without manual auth`() = runTest {
        verifyMethodCall(this) {
            getDocumentLayout("")
        }
    }

    @Test
    fun `getPaymentRequest works without manual auth`() = runTest {
        verifyMethodCall(this) {
            getPaymentRequest("")
        }
    }

    @Test
    fun `getPaymentRequests works without manual auth`() = runTest {
        verifyMethodCall(this) {
            getPaymentRequests()
        }
    }

    @Test
    fun `sendFeedback works without manual auth`() = runTest {
        verifyMethodCall(this) {
            sendFeedback("", "".toRequestBody())
        }
    }

    @Test
    fun `uploadDocument works without manual auth`() = runTest {
        verifyMethodCall(this) {
            uploadDocument(ByteArray(0), "", null, null, null)
        }
    }

    private inline fun verifyMethodCall(
        testScope: TestScope,
        testBlock: DocumentRemoteSource.() -> Unit
    ) {
        // Given
        val mockDocumentService = MockDocumentService()
        val testSubject =
            DocumentRemoteSourceForTests(
                StandardTestDispatcher(testScope.testScheduler),
                mockDocumentService,
                MockGiniApiType(),
                ""
            )

        // When
        with(testSubject) {
            testBlock()
        }
        testScope.advanceUntilIdle()

        // Then - verify method was called (no exceptions thrown)
        Truth.assertThat(mockDocumentService.called).isTrue()
    }

    private class MockDocumentService : DocumentService {

        var called = false

        override suspend fun uploadDocument(
            bytes: RequestBody,
            fileName: String?,
            docType: String?,
            headers: Map<String, String>
        ): Response<ResponseBody> {
            called = true
            return Response.success(null)
        }

        override suspend fun getDocument(documentId: String): Response<ResponseBody> {
            called = true
            return Response.success("response".toResponseBody())
        }

        override suspend fun getDocumentFromUri(uri: String): Response<ResponseBody> {
            called = true
            return Response.success("response".toResponseBody())
        }

        override suspend fun getExtractions(documentId: String): Response<ResponseBody> {
            called = true
            return Response.success("response".toResponseBody())
        }

        override suspend fun deleteDocument(documentId: String): Response<ResponseBody> {
            called = true
            return Response.success(null)
        }

        override suspend fun deleteDocumentFromUri(
            documentUri: Uri
        ): Response<ResponseBody> {
            called = true
            return Response.success(null)
        }

        override suspend fun getPaymentRequest(
            id: String
        ): Response<PaymentRequestResponse> {
            called = true
            return Response.success(PaymentRequestResponse(null, null, "", "", null, "", "", "", "", ""))
        }

        override suspend fun getPaymentRequests(): Response<List<PaymentRequestResponse>> {
            called = true
            return Response.success(listOf(PaymentRequestResponse(null, null, "", "", null, "", "", "", "", "")))
        }

        override suspend fun getPayment(
            id: String
        ): Response<PaymentResponse> {
            called = true
            return Response.success(PaymentResponse("", "", "", null, "", "" ))
        }

        override suspend fun getFile(location: String): Response<ResponseBody> {
            called = true
            return Response.success("response".toResponseBody())
        }

        override suspend fun sendFeedback(
            id: String,
            params: RequestBody
        ): Response<ResponseBody> {
            called = true
            return Response.success(null)
        }

        override suspend fun getDocumentLayout(
            documentId: String
        ): Response<DocumentLayoutResponse> {
            called = true
            return Response.success(DocumentLayoutResponse(emptyList()))
        }

        override suspend fun getDocumentPages(
            documentId: String
        ): Response<List<DocumentPageResponse>> {
            called = true
            return Response.success(null)
        }
    }
}