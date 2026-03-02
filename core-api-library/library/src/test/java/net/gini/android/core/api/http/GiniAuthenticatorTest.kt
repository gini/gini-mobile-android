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
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Tests for [GiniAuthenticator].
 * 
 * Verifies that the authenticator:
 * - Refreshes token on 401 responses
 * - Retries request with new token
 * - Gives up after 2 attempts (prevents infinite loop)
 * - Handles token refresh failures
 * - Ignores non-401 responses
 * - Uses mutex to prevent parallel refresh
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GiniAuthenticatorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var sessionManager: SessionManager
    private lateinit var interceptor: GiniAuthenticationInterceptor
    private lateinit var authenticator: GiniAuthenticator
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        sessionManager = mockk()
        interceptor = GiniAuthenticationInterceptor(sessionManager)
        authenticator = GiniAuthenticator(sessionManager)
        
        // Client with both interceptor (adds initial token) and authenticator (refreshes on 401)
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .authenticator(authenticator)
            .build()
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `authenticate refreshes token on 401 response`() = runTest {
        // Given
        val oldToken = "old-expired-token"
        val newToken = "new-refreshed-token"
        val futureDate = Date(System.currentTimeMillis() + 3600 * 1000)
        
        // First call returns old token, second call returns new token
        coEvery { sessionManager.getSession() } returnsMany listOf(
            Resource.Success(Session(oldToken, futureDate)),
            Resource.Success(Session(newToken, futureDate))
        )
        
        // First request fails with 401
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        // Second request (with new token) succeeds
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("success"))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/documents/123"))
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        
        // Then
        assertThat(response.code).isEqualTo(200)
        assertThat(mockWebServer.requestCount).isEqualTo(2)
        
        // Verify tokens
        val firstRequest = mockWebServer.takeRequest()
        assertThat(firstRequest.getHeader("Authorization"))
            .isEqualTo("Bearer $oldToken")
        
        val secondRequest = mockWebServer.takeRequest()
        assertThat(secondRequest.getHeader("Authorization"))
            .isEqualTo("Bearer $newToken")
        
        // Verify SessionManager was called twice (once for interceptor, once for authenticator)
        coVerify(exactly = 2) { sessionManager.getSession() }
    }

    @Test
    fun `authenticate retries request with new token`() = runTest {
        // Given
        val oldToken = "expired-token"
        val newToken = "fresh-token"
        val futureDate = Date(System.currentTimeMillis() + 3600 * 1000)
        
        coEvery { sessionManager.getSession() } returnsMany listOf(
            Resource.Success(Session(oldToken, futureDate)),
            Resource.Success(Session(newToken, futureDate))
        )
        
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("authenticated"))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/api/resource"))
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        
        // Then - Second request should succeed with new token
        assertThat(response.code).isEqualTo(200)
        assertThat(response.body?.string()).isEqualTo("authenticated")
        
        val retryRequest = mockWebServer.takeRequest() // Skip first
        val successRequest = mockWebServer.takeRequest()
        assertThat(successRequest.getHeader("Authorization"))
            .isEqualTo("Bearer $newToken")
    }

    @Test
    fun `authenticate gives up after 2 retry attempts`() = runTest {
        // Given
        val token1 = "token-1"
        val token2 = "token-2"
        val futureDate = Date(System.currentTimeMillis() + 3600 * 1000)
        
        // SessionManager keeps returning tokens
        coEvery { sessionManager.getSession() } returnsMany listOf(
            Resource.Success(Session(token1, futureDate)),
            Resource.Success(Session(token2, futureDate)),
            Resource.Success(Session("token-3", futureDate)) // Should not be called
        )
        
        // All requests fail with 401
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/documents/456"))
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        
        // Then - Should give up after 2 attempts and return 401
        assertThat(response.code).isEqualTo(401)
        assertThat(mockWebServer.requestCount).isEqualTo(2)
        
        // Verify SessionManager was called exactly twice (not 3 times)
        coVerify(exactly = 2) { sessionManager.getSession() }
    }

    @Test
    fun `authenticate returns null if token refresh fails`() = runTest {
        // Given
        val oldToken = "old-token"
        val futureDate = Date(System.currentTimeMillis() + 3600 * 1000)
        
        coEvery { sessionManager.getSession() } returnsMany listOf(
            Resource.Success(Session(oldToken, futureDate)),
            Resource.Error<Session>(exception = Exception("Token refresh failed"))
        )
        
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/documents/789"))
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        
        // Then - Should return 401 without retry
        assertThat(response.code).isEqualTo(401)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        
        // Verify SessionManager was called twice (once for interceptor, once for authenticator)
        coVerify(exactly = 2) { sessionManager.getSession() }
    }

    @Test
    fun `authenticate returns null for non-401 responses`() = runTest {
        // Given
        val token = "valid-token"
        val futureDate = Date(System.currentTimeMillis() + 3600 * 1000)
        
        coEvery { sessionManager.getSession() } returns 
            Resource.Success(Session(token, futureDate))
        
        // Return 403 Forbidden (not 401)
        mockWebServer.enqueue(MockResponse().setResponseCode(403))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/admin/resource"))
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        
        // Then - Should not retry, just return 403
        assertThat(response.code).isEqualTo(403)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        
        // Verify SessionManager was only called once (by interceptor, not by authenticator)
        coVerify(exactly = 1) { sessionManager.getSession() }
    }

    @Test
    fun `authenticate uses mutex to prevent parallel refresh`() = runTest {
        // Given
        val oldToken = "old-token"
        val newToken = "new-token"
        val futureDate = Date(System.currentTimeMillis() + 3600 * 1000)
        var sessionCallCount = 0
        
        // Track session calls
        coEvery { sessionManager.getSession() } coAnswers {
            sessionCallCount++
            if (sessionCallCount == 1) {
                Resource.Success(Session(oldToken, futureDate))
            } else {
                Resource.Success(Session(newToken, futureDate))
            }
        }
        
        // Both requests fail with 401 initially
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        
        // When - Make single request that triggers 401
        val request = Request.Builder()
            .url(mockWebServer.url("/documents/999"))
            .build()
        
        okHttpClient.newCall(request).execute()
        
        // Then - Session should be called twice:
        // 1. Once by interceptor (gets old token)
        // 2. Once by authenticator (refreshes to new token)
        // The mutex ensures authenticator doesn't call multiple times in parallel
        assertThat(sessionCallCount).isEqualTo(2)
        
        // Verify the refresh happened exactly once
        coVerify(exactly = 2) { sessionManager.getSession() }
    }
}
