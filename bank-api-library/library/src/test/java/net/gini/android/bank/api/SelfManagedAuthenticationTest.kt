package net.gini.android.bank.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.authorization.Session
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.authorization.UserCredentials
import net.gini.android.core.api.http.GiniHttpClientProvider
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Tests for self-managed authentication (PP-2363): the consumer's OkHttpClient authenticates
 * the API requests and neither a SessionManager nor client credentials are required.
 */
@RunWith(AndroidJUnit4::class)
class SelfManagedAuthenticationTest {

    private lateinit var server: MockWebServer

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun builderWithProvider(provider: GiniHttpClientProvider): GiniBankAPIBuilder =
        GiniBankAPIBuilder(context)
            .setApiBaseUrl(server.url("/").toString())
            .setHttpClientProvider(provider)
            .setSelfManagedAuthentication(true)
            .setCredentialsStore(InMemoryCredentialsStore()) as GiniBankAPIBuilder

    @Test
    fun `api requests are authenticated by the consumer's own interceptor`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(DOCUMENT_JSON))

        val consumerAuthProvider = GiniHttpClientProvider {
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("Authorization", "Bearer consumer-token-42")
                            .build()
                    )
                }
                .build()
        }
        val giniBankApi = builderWithProvider(consumerAuthProvider).build()

        val resource = giniBankApi.documentManager.getDocument("document-id-13")

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        val request = server.takeRequest()
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer consumer-token-42")
        // No user center or other traffic: the SDK never requested a session
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `api requests are sent without authorization header when the consumer does not authenticate them`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401).setBody("""{"message":"unauthorized"}""")
        )

        val plainProvider = GiniHttpClientProvider { OkHttpClient() }
        val giniBankApi = builderWithProvider(plainProvider).build()

        val resource = giniBankApi.documentManager.getDocument("document-id-13")

        // The SDK does not interfere: the request goes out unauthenticated and the API's
        // error response is mapped to an error resource
        val request = server.takeRequest()
        assertThat(request.getHeader("Authorization")).isNull()
        val error = resource as Resource.Error
        assertThat(error.responseStatusCode).isEqualTo(401)
    }

    @Test
    fun `a SessionManager passed to the builder is ignored when authentication is self-managed`() = runTest {
        val passedSessionManager = SessionManager {
            Resource.Success(Session("must-never-be-used", Date(Date().time + 60_000)))
        }
        val builder = GiniBankAPIBuilder(context, sessionManager = passedSessionManager)
            .setApiBaseUrl(server.url("/").toString())
            .setHttpClientProvider(GiniHttpClientProvider { OkHttpClient() })
            .setSelfManagedAuthentication(true)
            .setCredentialsStore(InMemoryCredentialsStore()) as GiniBankAPIBuilder

        // The sentinel takes precedence over the passed SessionManager: anything still asking
        // for a session (e.g. the deprecated withAccessToken) must get an error instead of
        // silently routing the consumer's token through the SDK.
        val sessionResource = builder.getSessionManager().getSession()

        val error = sessionResource as Resource.Error
        assertThat(error.message).contains("self-managed")
    }

    @Test
    fun `building with self-managed authentication fails without a custom http client provider`() {
        val builder = GiniBankAPIBuilder(context)
            .setApiBaseUrl(server.url("/").toString())
            .setSelfManagedAuthentication(true)
            .setCredentialsStore(InMemoryCredentialsStore()) as GiniBankAPIBuilder

        val exception = assertThrows(IllegalStateException::class.java) {
            builder.build()
        }
        assertThat(exception).hasMessageThat().contains("GiniHttpClientProvider")
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

    companion object {
        private val DOCUMENT_JSON = """
            {
                "id": "document-id-13",
                "progress": "COMPLETED",
                "pageCount": 1,
                "name": "invoice.jpg",
                "creationDate": 1515932941283,
                "sourceClassification": "NATIVE",
                "_links": {
                    "document": "https://pay-api.gini.net/documents/document-id-13",
                    "extractions": "https://pay-api.gini.net/documents/document-id-13/extractions"
                }
            }
        """.trimIndent()
    }
}
