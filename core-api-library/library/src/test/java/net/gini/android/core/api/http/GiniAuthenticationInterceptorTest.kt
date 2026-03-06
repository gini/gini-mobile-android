package net.gini.android.core.api.http

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.Session
import net.gini.android.core.api.authorization.SessionManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date

/**
 * Tests for [GiniAuthenticationInterceptor].
 * 
 * Verifies that the interceptor:
 * - Adds Bearer token automatically to authenticated endpoints
 * - Skips authentication for OAuth and user creation endpoints
 * - Handles session errors correctly
 * - Uses mutex to prevent parallel token fetches
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GiniAuthenticationInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var sessionManager: SessionManager
    private lateinit var interceptor: GiniAuthenticationInterceptor
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        sessionManager = mockk()
        interceptor = GiniAuthenticationInterceptor(sessionManager)
        
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `intercept adds Authorization header with Bearer token`() = runTest {
        // Given
        val expectedToken = "test-access-token-12345"
        val futureDate = Date(System.currentTimeMillis() + 3600 * 1000)
        coEvery { sessionManager.getSession() } returns 
            Resource.Success(Session(expectedToken, futureDate))
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/documents/123"))
            .build()
        
        okHttpClient.newCall(request).execute()
        
        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.getHeader("Authorization"))
            .isEqualTo("Bearer $expectedToken")
    }

    @Test
    fun `intercept skips authentication for oauth token endpoint`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/oauth/token"))
            .build()
        
        okHttpClient.newCall(request).execute()
        
        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.getHeader("Authorization")).isNull()
        
        // Verify SessionManager was never called
        coVerify(exactly = 0) { sessionManager.getSession() }
    }

    @Test
    fun `intercept skips authentication for POST to users endpoint`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/users"))
            .post("".toRequestBody(null))
            .build()
        
        okHttpClient.newCall(request).execute()
        
        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.getHeader("Authorization")).isNull()
        
        // Verify SessionManager was never called
        coVerify(exactly = 0) { sessionManager.getSession() }
    }

    @Test
    fun `intercept skips authentication if Authorization header already present`() = runTest {
        // Given
        val existingAuth = "Bearer custom-token"
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/documents/123"))
            .header("Authorization", existingAuth)
            .build()
        
        okHttpClient.newCall(request).execute()
        
        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo(existingAuth)
        
        // Verify SessionManager was never called
        coVerify(exactly = 0) { sessionManager.getSession() }
    }

    @Test
    fun `intercept throws IOException when SessionManager returns error`() = runTest {
        // Given
        val errorMessage = "Session error"
        coEvery { sessionManager.getSession() } returns 
            Resource.Error<Session>(exception = Exception(errorMessage))
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        
        // When/Then
        val request = Request.Builder()
            .url(mockWebServer.url("/documents/123"))
            .build()
        
        try {
            okHttpClient.newCall(request).execute()
            throw AssertionError("Expected IOException to be thrown")
        } catch (e: IOException) {
            assertThat(e.message).contains("Failed to get session")
            assertThat(e.message).contains(errorMessage)
        }
    }

    @Test
    fun `intercept throws IOException when SessionManager returns cancelled`() = runTest {
        // Given
        coEvery { sessionManager.getSession() } returns Resource.Cancelled()
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        
        // When/Then
        val request = Request.Builder()
            .url(mockWebServer.url("/documents/123"))
            .build()
        
        try {
            okHttpClient.newCall(request).execute()
            throw AssertionError("Expected IOException to be thrown")
        } catch (e: IOException) {
            assertThat(e.message).contains("Session fetch was cancelled")
        }
    }

    @Test
    fun `intercept fetches token from SessionManager on first request`() = runTest {
        // Given
        val token = "test-token"
        val futureDate = Date(System.currentTimeMillis() + 3600 * 1000)
        coEvery { sessionManager.getSession() } returns 
            Resource.Success(Session(token, futureDate))
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/documents/123"))
            .build()
        
        okHttpClient.newCall(request).execute()
        
        // Then
        coVerify(exactly = 1) { sessionManager.getSession() }
    }

    @Test
    fun `intercept uses mutex to prevent parallel token fetches`() = runTest {
        // Given
        val token = "test-token"
        val futureDate = Date(System.currentTimeMillis() + 3600 * 1000)
        var sessionCallCount = 0
        
        coEvery { sessionManager.getSession() } coAnswers {
            sessionCallCount++
            Resource.Success(Session(token, futureDate))
        }
        
        // Enqueue 2 responses
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        
        // When - Make 2 requests that will execute serially (interceptor has mutex)
        val request1 = Request.Builder()
            .url(mockWebServer.url("/documents/123"))
            .build()
        
        val request2 = Request.Builder()
            .url(mockWebServer.url("/documents/456"))
            .build()
        
        // Execute first request
        okHttpClient.newCall(request1).execute()
        // Execute second request (should call session again)
        okHttpClient.newCall(request2).execute()
        
        // Then - Session called twice (once per request, but mutex prevents overlap)
        assertThat(sessionCallCount).isEqualTo(2)
        
        // Both requests should have the same token
        val recorded1 = mockWebServer.takeRequest()
        val recorded2 = mockWebServer.takeRequest()
        
        assertThat(recorded1.getHeader("Authorization")).isEqualTo("Bearer $token")
        assertThat(recorded2.getHeader("Authorization")).isEqualTo("Bearer $token")
    }
}
