package net.gini.android.core.api.internal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.DocumentRemoteSource
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.DocumentService
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.AnonymousSessionManager
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.authorization.Session
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.authorization.UserCredentials
import net.gini.android.core.api.http.GiniHttpClientProvider
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.core.api.test.DocumentRemoteSourceForTests
import net.gini.android.core.api.test.WireTestGiniApiType
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date

/**
 * Tests for the client composition done by [GiniCoreAPIBuilder]: which OkHttp clients get the
 * session interceptor, how self-managed authentication changes the composition, and the
 * isolation of the User Center API client (PP-2363).
 */
@RunWith(AndroidJUnit4::class)
class GiniCoreAPIBuilderTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

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

    private fun builder(sessionManager: SessionManager? = null): TestCoreApiBuilder =
        TestCoreApiBuilder(context, sessionManager = sessionManager).apply {
            setApiBaseUrl(server.url("/").toString())
        }

    private fun successfulSessionManager(accessToken: String = "token-1234") =
        SessionManager { Resource.Success(Session(accessToken, Date(Date().time + 60_000))) }

    @Test
    fun `building with self-managed authentication fails without a custom http client provider`() {
        val builder = builder().apply { setSelfManagedAuthentication(true) }

        val exception = assertThrows(IllegalStateException::class.java) { builder.build() }

        assertThat(exception).hasMessageThat().contains("GiniHttpClientProvider")
    }

    @Test
    fun `getSessionManager returns an erroring session manager when authentication is self-managed`() = runTest {
        val builder = builder(sessionManager = successfulSessionManager()).apply {
            setSelfManagedAuthentication(true)
            setHttpClientProvider { OkHttpClient() }
        }

        // Even though a SessionManager was passed to the constructor it must not be used:
        // the SDK never requests sessions in self-managed authentication mode
        val session = builder.getSessionManager().getSession()

        assertThat(session).isInstanceOf(Resource.Error::class.java)
        assertThat((session as Resource.Error).message).contains("self-managed")
    }

    @Test
    fun `getSessionManager returns the session manager passed to the constructor`() {
        val sessionManager = successfulSessionManager()

        assertThat(builder(sessionManager = sessionManager).getSessionManager())
            .isSameInstanceAs(sessionManager)
    }

    @Test
    fun `getSessionManager defaults to an anonymous session manager`() {
        val builder = builder().apply { setCredentialsStore(InMemoryCredentialsStore()) }

        assertThat(builder.getSessionManager()).isInstanceOf(AnonymousSessionManager::class.java)
    }

    @Test
    fun `api requests are authenticated by the session interceptor`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val builder = builder(sessionManager = successfulSessionManager("session-token-xyz"))

        builder.apiOkHttpClient()
            .newCall(Request.Builder().url(server.url("/documents/1")).build())
            .execute()
            .close()

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer session-token-xyz")
    }

    @Test
    fun `the session interceptor and user agent are added on top of the consumer's http client`() {
        server.enqueue(MockResponse().setResponseCode(200))
        var consumerInterceptorRan = false
        val consumerClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                consumerInterceptorRan = true
                chain.proceed(chain.request())
            }
            .build()
        val builder = builder(sessionManager = successfulSessionManager()).apply {
            setHttpClientProvider { consumerClient }
        }

        builder.apiOkHttpClient()
            .newCall(Request.Builder().url(server.url("/documents/1")).build())
            .execute()
            .close()

        val request = server.takeRequest()
        assertThat(consumerInterceptorRan).isTrue()
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer token-1234")
        assertThat(request.getHeader("User-Agent")).isNotEmpty()
    }

    @Test
    fun `tracking analytics requests are authenticated by the session interceptor`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val builder = builder(sessionManager = successfulSessionManager("session-token-xyz"))

        builder.trackingOkHttpClient()
            .newCall(Request.Builder().url(server.url("/events/batch")).build())
            .execute()
            .close()

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer session-token-xyz")
    }

    @Test
    fun `user center token requests do not depend on the consumer client's dispatcher`() {
        // Regression test for a potential deadlock: the session interceptor blocks API calls
        // (and their dispatcher slots) while it fetches a token through the User Center client.
        // If the User Center client shared the consumer client's Dispatcher and all its slots
        // were taken by blocked API calls, the token request could never start.
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = MockResponse().setResponseCode(500)
        }
        val parkServer = MockWebServer().apply {
            start()
            enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        }
        val consumerDispatcher = Dispatcher().apply {
            maxRequests = 1
            maxRequestsPerHost = 1
        }
        val consumerClient = OkHttpClient.Builder().dispatcher(consumerDispatcher).build()

        // Occupy the consumer dispatcher's only slot with a request which never gets a response
        val parkedCall = consumerClient.newCall(Request.Builder().url(parkServer.url("/park")).build())
        parkedCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = Unit
            override fun onResponse(call: Call, response: Response) = response.close()
        })
        awaitTrue { consumerDispatcher.runningCallsCount() == 1 }

        val builder = builder().apply {
            setUserCenterApiBaseUrl(server.url("/").toString())
            setHttpClientProvider { consumerClient }
            setCredentialsStore(InMemoryCredentialsStore())
        }

        try {
            // Completes only when the token request runs on its own dispatcher; the response
            // shape does not matter - the mocked 500 maps to an error resource
            val session = runBlocking {
                withTimeout(5_000) { builder.getSessionManager().getSession() }
            }
            assertThat(session).isInstanceOf(Resource.Error::class.java)
        } finally {
            parkedCall.cancel()
            parkServer.shutdown()
        }
    }

    private fun awaitTrue(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Condition not met within 5s" }
            Thread.sleep(10)
        }
    }

    // --- Minimal concrete types for the abstract builder ---

    private class TestCoreApiBuilder(
        context: Context,
        clientId: String = "test-client-id",
        clientSecret: String = "test-client-secret",
        emailDomain: String = "example.com",
        sessionManager: SessionManager? = null
    ) : GiniCoreAPIBuilder<TestDocumentManager, TestGiniCoreAPI, TestDocumentRepository, ExtractionsContainer>(
        context, clientId, clientSecret, emailDomain, sessionManager
    ) {

        override fun getGiniApiType(): GiniApiType = WireTestGiniApiType()

        override fun build(): TestGiniCoreAPI = TestGiniCoreAPI(createDocumentManager(), getCredentialsStore())

        override fun createDocumentManager(): TestDocumentManager = TestDocumentManager(getDocumentRepository())

        override fun createDocumentRepository(): TestDocumentRepository =
            TestDocumentRepository(
                DocumentRemoteSourceForTests(
                    Dispatchers.IO,
                    getApiRetrofit().create(DocumentService::class.java),
                    getGiniApiType(),
                    getApiBaseUrl() ?: ""
                ),
                getSessionManager(),
                getGiniApiType()
            )

        fun apiOkHttpClient(): OkHttpClient = getApiRetrofit().callFactory() as OkHttpClient

        fun trackingOkHttpClient(): OkHttpClient = getTrackingAnalyticsApiRetrofit().callFactory() as OkHttpClient
    }

    private class TestGiniCoreAPI(documentManager: TestDocumentManager, credentialsStore: CredentialsStore) :
        GiniCoreAPI<TestDocumentManager, TestDocumentRepository, ExtractionsContainer>(
            documentManager, credentialsStore
        )

    private class TestDocumentManager(documentRepository: TestDocumentRepository) :
        DocumentManager<TestDocumentRepository, ExtractionsContainer>(documentRepository)

    private class TestDocumentRepository(
        remoteSource: DocumentRemoteSource,
        sessionManager: SessionManager,
        apiType: GiniApiType
    ) : DocumentRepository<ExtractionsContainer>(remoteSource, sessionManager, apiType) {

        override fun createExtractionsContainer(
            specificExtractions: Map<String, SpecificExtraction>,
            compoundExtractions: Map<String, CompoundExtraction>,
            responseJSON: JSONObject
        ): ExtractionsContainer = ExtractionsContainer(specificExtractions, compoundExtractions)
    }

    private class InMemoryCredentialsStore : CredentialsStore {
        private var credentials: UserCredentials? = null

        override fun storeUserCredentials(userCredentials: UserCredentials?): Boolean {
            credentials = userCredentials
            return true
        }

        override fun getUserCredentials(): UserCredentials? = credentials

        override fun deleteUserCredentials(): Boolean {
            credentials = null
            return true
        }
    }
}
