package net.gini.android.core.api.http

import net.gini.android.core.api.GiniApiType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests for [GiniApiHeaderInterceptor].
 */
class GiniApiHeaderInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var giniApiType: TestGiniApiType
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        giniApiType = TestGiniApiType()
        // Note: Most tests use addInterceptor for simplicity since we're testing without Retrofit.
        // See the `network interceptor validates proper header replacement order` test for
        // validation of production network interceptor behavior.
        client = OkHttpClient.Builder()
            .addInterceptor(GiniApiHeaderInterceptor(giniApiType))
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `does not add Accept header`() {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        // When
        client.newCall(request).execute().use { }

        // Then - Accept header should not be added by interceptor
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals(null, recordedRequest.getHeader("Accept"))
    }

    @Test
    fun `adds Content-Type header when not present`() {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        // When
        client.newCall(request).execute().use { }

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("application/vnd.gini.v4+json", recordedRequest.getHeader("Content-Type"))
    }

    @Test
    fun `preserves explicitly set Accept header`() {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .header("Accept", "application/json")
            .build()

        // When
        client.newCall(request).execute().use { }

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun `does not override existing Content-Type header`() {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .header("Content-Type", "text/plain")
            .build()

        // When
        client.newCall(request).execute().use { }

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("text/plain", recordedRequest.getHeader("Content-Type"))
    }

    @Test
    fun `replaces application json Content-Type with versioned media type`() {
        // Given - simulates what Retrofit's Moshi converter does
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .header("Content-Type", "application/json; charset=UTF-8")
            .build()

        // When
        client.newCall(request).execute().use { }

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("application/vnd.gini.v4+json", recordedRequest.getHeader("Content-Type"))
    }

    @Test
    fun `uses dynamic API version from GiniApiType`() {
        // Given
        val customApiType = TestGiniApiType(apiVersion = 5)
        val customClient = OkHttpClient.Builder()
            .addInterceptor(GiniApiHeaderInterceptor(customApiType))
            .build()
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        // When
        customClient.newCall(request).execute().use { }

        // Then - Only Content-Type should use dynamic version
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals(null, recordedRequest.getHeader("Accept"))
        assertEquals("application/vnd.gini.v5+json", recordedRequest.getHeader("Content-Type"))
    }

    @Test
    fun `network interceptor validates proper header replacement order`() {
        // Given - Using network interceptor like production
        // This test validates that network interceptors run AFTER application interceptors,
        // which is critical for replacing Retrofit's default headers.
        //
        // This validates the production interceptor ordering documented in GiniCoreAPIBuilder:
        // 1. Consumer's application interceptors (cloned from provided client)
        // 2. SDK's application interceptor (GiniAuthenticationInterceptor - adds auth)
        // 3. SDK's network interceptor (GiniApiHeaderInterceptor - replaces headers)
        // 4. SDK's authenticator (GiniAuthenticator - handles 401 refresh)
        val networkClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Simulate what Retrofit does (application interceptor adds default headers)
                val request = chain.request().newBuilder()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .build()
                chain.proceed(request)
            }
            .addNetworkInterceptor(GiniApiHeaderInterceptor(giniApiType))
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        // When
        networkClient.newCall(request).execute().use { }

        // Then - Network interceptor should replace the application/json header
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("application/vnd.gini.v4+json", recordedRequest.getHeader("Content-Type"))
    }

    private class TestGiniApiType(private val apiVersion: Int = 4) : GiniApiType {
        override val baseUrl: String = "https://api.gini.net/"
        override val giniJsonMediaType: String = "application/vnd.gini.v$apiVersion+json"
        override val giniPartialMediaType: String = "application/vnd.gini.v$apiVersion.partial"
        override val giniCompositeJsonMediaType: String = "application/vnd.gini.v$apiVersion.composite+json"
    }
}
