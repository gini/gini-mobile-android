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
        // Note: Using addInterceptor for tests (not addNetworkInterceptor like production)
        // Both work here since we're testing without Retrofit. In production, we use
        // addNetworkInterceptor to run after Retrofit adds its default headers.
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

    private class TestGiniApiType(private val apiVersion: Int = 4) : GiniApiType {
        override val baseUrl: String = "https://api.gini.net/"
        override val giniJsonMediaType: String = "application/vnd.gini.v$apiVersion+json"
        override val giniPartialMediaType: String = "application/vnd.gini.v$apiVersion.partial"
        override val giniCompositeJsonMediaType: String = "application/vnd.gini.v$apiVersion.composite+json"
    }
}
