package net.gini.android.health.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.CredentialsStore
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

    @Test
    fun `api requests are authenticated by the consumer's own interceptor`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                    "clientID": "client-id-1",
                    "communicationTone": "FORMAL",
                    "ingredientBrandType": "FULL_VISIBLE"
                }
                """.trimIndent()
            )
        )

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
        val giniHealthApi = GiniHealthAPIBuilder(context)
            .setApiBaseUrl(server.url("/").toString())
            .setHttpClientProvider(consumerAuthProvider)
            .setSelfManagedAuthentication(true)
            .setCredentialsStore(InMemoryCredentialsStore())
            .let { it as GiniHealthAPIBuilder }
            .build()

        val resource = giniHealthApi.documentManager.getConfigurations()

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
        val giniHealthApi = GiniHealthAPIBuilder(context)
            .setApiBaseUrl(server.url("/").toString())
            .setHttpClientProvider(plainProvider)
            .setSelfManagedAuthentication(true)
            .setCredentialsStore(InMemoryCredentialsStore())
            .let { it as GiniHealthAPIBuilder }
            .build()

        val resource = giniHealthApi.documentManager.getConfigurations()

        // The SDK does not interfere: the request goes out unauthenticated and the API's
        // error response is mapped to an error resource
        val request = server.takeRequest()
        assertThat(request.getHeader("Authorization")).isNull()
        val error = resource as Resource.Error
        assertThat(error.responseStatusCode).isEqualTo(401)
    }

    @Test
    fun `building with self-managed authentication fails without a custom http client provider`() {
        val builder = GiniHealthAPIBuilder(context)
            .setApiBaseUrl(server.url("/").toString())
            .setSelfManagedAuthentication(true)
            .setCredentialsStore(InMemoryCredentialsStore()) as GiniHealthAPIBuilder

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
}
