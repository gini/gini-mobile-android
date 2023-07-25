package net.gini.android.core.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.response.PaymentRequestResponse
import net.gini.android.core.api.test.DocumentRemoteSourceForTests
import net.gini.android.core.api.test.MockGiniApiType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DocumentRemoteSourceTest {

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in deleteDocument with document id`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            deleteDocument(accessToken, "")
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in deleteDocument with uri`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            deleteDocument(accessToken, Uri.EMPTY)
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getDocument`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            getDocument(accessToken, "")
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getDocumentFromUri`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            getDocumentFromUri(accessToken, Uri.EMPTY)
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getExtractions`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            getExtractions(accessToken, "")
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getFile`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            getFile(accessToken, "")
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getLayout`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            getLayout(accessToken, "")
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getPaymentRequest`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            getPaymentRequest(accessToken, "")
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in getPaymentRequests`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            getPaymentRequests(accessToken)
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in sendFeedback`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            sendFeedback(accessToken, "", "".toRequestBody())
        }
    }

    @Test
    fun `sets bearer authorization header with capital case 'Bearer' in uploadDocument`() = runTest {
        val accessToken = UUID.randomUUID().toString()
        val expectedAuthorizationHeader = "Bearer $accessToken"
        verifyAuthorizationHeader(expectedAuthorizationHeader, this) {
            uploadDocument(accessToken, ByteArray(0), "", null, null, null)
        }
    }

    private inline fun verifyAuthorizationHeader(
        expectedAuthorizationHeader: String,
        testScope: TestScope,
        testBlock: DocumentRemoteSource.() -> Unit
    ) {
        // Given
        val documentServiceAuthInterceptor = DocumentServiceAuthInterceptor()
        val testSubject =
            DocumentRemoteSourceForTests(
                StandardTestDispatcher(testScope.testScheduler),
                documentServiceAuthInterceptor,
                MockGiniApiType(),
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

    private class DocumentServiceAuthInterceptor : DocumentService {

        var bearerAuthHeader: String? = null

        override suspend fun uploadDocument(
            bearer: Map<String, String>,
            bytes: RequestBody,
            fileName: String?,
            docType: String?
        ): Response<ResponseBody> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(null)
        }

        override suspend fun getDocument(bearer: Map<String, String>, documentId: String): Response<ResponseBody> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success("response".toResponseBody())
        }

        override suspend fun getDocumentFromUri(bearer: Map<String, String>, uri: String): Response<ResponseBody> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success("response".toResponseBody())
        }

        override suspend fun getExtractions(bearer: Map<String, String>, documentId: String): Response<ResponseBody> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success("response".toResponseBody())
        }

        override suspend fun deleteDocument(bearer: Map<String, String>, documentId: String): Response<ResponseBody> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(null)
        }

        override suspend fun deleteDocumentFromUri(
            bearer: Map<String, String>,
            documentUri: Uri
        ): Response<ResponseBody> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(null)
        }

        override suspend fun getLayoutForDocument(
            bearer: Map<String, String>,
            documentId: String
        ): Response<ResponseBody> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success("response".toResponseBody())
        }

        override suspend fun getPaymentRequest(
            bearer: Map<String, String>,
            id: String
        ): Response<PaymentRequestResponse> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(PaymentRequestResponse(null, null, "", "", null, "", "", ""))
        }

        override suspend fun getPaymentRequests(bearer: Map<String, String>): Response<List<PaymentRequestResponse>> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(listOf(PaymentRequestResponse(null, null, "", "", null, "", "", "")))
        }

        override suspend fun getFile(bearer: Map<String, String>, location: String): Response<ResponseBody> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success("response".toResponseBody())
        }

        override suspend fun sendFeedback(
            bearer: Map<String, String>,
            id: String,
            params: RequestBody
        ): Response<ResponseBody> {
            bearerAuthHeader = bearer["Authorization"]
            return Response.success(null)
        }
    }
}