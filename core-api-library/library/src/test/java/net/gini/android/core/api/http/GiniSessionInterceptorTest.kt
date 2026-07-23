package net.gini.android.core.api.http

import com.google.common.truth.Truth.assertThat
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.Session
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.SessionCancellationException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class GiniSessionInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun clientWith(sessionManager: SessionManager): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(GiniSessionInterceptor { sessionManager })
            .build()

    private fun successfulSessionManager(accessToken: String = ACCESS_TOKEN) =
        SessionManager { Resource.Success(Session(accessToken, Date(Date().time + 60_000))) }

    private fun request(): Request = Request.Builder().url(server.url("/documents/1")).build()

    @Test
    fun `adds bearer authorization header from the session manager when the request has none`() {
        server.enqueue(MockResponse().setResponseCode(200))

        clientWith(successfulSessionManager("session-token-xyz")).newCall(request()).execute().use { response ->
            assertThat(response.code).isEqualTo(200)
        }

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer session-token-xyz")
    }

    @Test
    fun `passes the request through untouched when it already has an authorization header`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val sessionManagerCallCount = AtomicInteger(0)
        val sessionManager = SessionManager {
            sessionManagerCallCount.incrementAndGet()
            Resource.Success(Session(ACCESS_TOKEN, Date(Date().time + 60_000)))
        }

        val requestWithAuth = Request.Builder()
            .url(server.url("/oauth/token"))
            // Dummy credential, not a real secret: base64 of "client:secret". Any non-empty
            // value works here; the test only checks that an existing header is preserved.
            .header("Authorization", "Basic Y2xpZW50OnNlY3JldA==")
            .build()
        clientWith(sessionManager).newCall(requestWithAuth).execute().close()

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Basic Y2xpZW50OnNlY3JldA==")
        assertThat(sessionManagerCallCount.get()).isEqualTo(0)
    }

    @Test
    fun `throws ApiException with the session error fields when the session request fails`() {
        val sessionManager = SessionManager {
            Resource.Error(
                message = "user authentication failed",
                responseStatusCode = 401,
                responseHeaders = mapOf("www-authenticate" to listOf("Bearer")),
                responseBody = """{"error":"invalid_grant"}"""
            )
        }

        try {
            clientWith(sessionManager).newCall(request()).execute()
            fail("Expected ApiException")
        } catch (e: ApiException) {
            assertThat(e.message).isEqualTo("user authentication failed")
            assertThat(e.responseStatusCode).isEqualTo(401)
            assertThat(e.responseHeaders).isEqualTo(mapOf("www-authenticate" to listOf("Bearer")))
            assertThat(e.responseBody).isEqualTo("""{"error":"invalid_grant"}""")
        }

        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `throws SessionCancellationException when the session request is cancelled`() {
        val sessionManager = SessionManager { Resource.Cancelled() }

        try {
            clientWith(sessionManager).newCall(request()).execute()
            fail("Expected SessionCancellationException")
        } catch (e: SessionCancellationException) {
            // expected
        }

        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `wraps exceptions thrown by the session manager in an ApiException`() {
        // A SessionManager should return Resource errors but may throw anything. Only
        // IOExceptions may leave an interceptor: OkHttp's async path rethrows anything else
        // on its dispatcher thread which crashes the app.
        val sessionManager = SessionManager { throw IllegalStateException("token backend exploded") }

        try {
            clientWith(sessionManager).newCall(request()).execute()
            fail("Expected ApiException")
        } catch (e: ApiException) {
            assertThat(e.message).isEqualTo("Session request failed: token backend exploded")
            assertThat(e.cause).isInstanceOf(IllegalStateException::class.java)
        }

        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `maps a CancellationException thrown by the session manager to a SessionCancellationException`() {
        val sessionManager = SessionManager { throw CancellationException("session scope was cancelled") }

        try {
            clientWith(sessionManager).newCall(request()).execute()
            fail("Expected SessionCancellationException")
        } catch (e: SessionCancellationException) {
            // expected
        }

        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `serializes parallel session requests with a mutex`() {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(200)) }
        val concurrentSessions = AtomicInteger(0)
        val maxConcurrentSessions = AtomicInteger(0)
        val sessionManager = SessionManager {
            val current = concurrentSessions.incrementAndGet()
            maxConcurrentSessions.updateAndGet { max -> maxOf(max, current) }
            Thread.sleep(100)
            concurrentSessions.decrementAndGet()
            Resource.Success(Session(ACCESS_TOKEN, Date(Date().time + 60_000)))
        }
        val client = clientWith(sessionManager)

        val finished = CountDownLatch(3)
        repeat(3) {
            Thread {
                client.newCall(request()).execute().close()
                finished.countDown()
            }.start()
        }
        assertThat(finished.await(10, TimeUnit.SECONDS)).isTrue()

        assertThat(server.requestCount).isEqualTo(3)
        // Only one session request at a time: prevents creating multiple anonymous users when
        // multiple documents are uploaded in parallel on first use.
        assertThat(maxConcurrentSessions.get()).isEqualTo(1)
    }

    @Test
    fun `resolves the session manager lazily on the first request`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val providerCallCount = AtomicInteger(0)
        val client = OkHttpClient.Builder()
            .addInterceptor(GiniSessionInterceptor {
                providerCallCount.incrementAndGet()
                successfulSessionManager()
            })
            .build()

        assertThat(providerCallCount.get()).isEqualTo(0)

        client.newCall(request()).execute().close()
        assertThat(providerCallCount.get()).isEqualTo(1)

        server.enqueue(MockResponse().setResponseCode(200))
        client.newCall(request()).execute().close()
        // The provider is called only once; the session manager instance is reused
        assertThat(providerCallCount.get()).isEqualTo(1)
    }

    companion object {
        private const val ACCESS_TOKEN = "test-access-token-1234"
    }
}
