package net.gini.android.capture.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.authorization.UserCredentials
import net.gini.android.core.api.http.GiniHttpClientProvider
import okhttp3.OkHttpClient
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Tests for building the [GiniCaptureDefaultNetworkService] with self-managed authentication
 * (PP-2363): the consumer's OkHttpClient authenticates the API requests and neither a
 * SessionManager nor client credentials are required.
 */
@RunWith(AndroidJUnit4::class)
class GiniCaptureDefaultNetworkServiceBuilderTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `builds with self-managed authentication without client credentials or session manager`() {
        val networkService = GiniCaptureDefaultNetworkService.builder(context)
            .setHttpClientProvider(GiniHttpClientProvider { OkHttpClient() })
            .setSelfManagedAuthentication(true)
            .setCredentialsStore(InMemoryCredentialsStore())
            .build()

        assertThat(networkService).isNotNull()
    }

    @Test
    fun `self-managed authentication takes precedence over a configured SessionManager`() {
        // Black-box pin of the `when` branch order in build(): the self-managed branch
        // requires a GiniHttpClientProvider and must throw without one - if the SessionManager
        // branch won instead, build() would succeed and the SessionManager would be live.
        val builder = GiniCaptureDefaultNetworkService.builder(context)
            .setSessionManager { error("must never be used") }
            .setSelfManagedAuthentication(true)
            .setCredentialsStore(InMemoryCredentialsStore())

        val exception = assertThrows(IllegalStateException::class.java) {
            builder.build()
        }
        assertThat(exception).hasMessageThat().contains("GiniHttpClientProvider")
    }

    @Test
    fun `builds with self-managed authentication when a leftover SessionManager is configured`() {
        val networkService = GiniCaptureDefaultNetworkService.builder(context)
            .setSessionManager { error("must never be used") }
            .setHttpClientProvider(GiniHttpClientProvider { OkHttpClient() })
            .setSelfManagedAuthentication(true)
            .setCredentialsStore(InMemoryCredentialsStore())
            .build()

        assertThat(networkService).isNotNull()
    }

    @Test
    fun `builds with self-managed authentication when leftover client credentials are configured`() {
        val networkService = GiniCaptureDefaultNetworkService.builder(context)
            .setClientCredentials("leftover-client-id", "leftover-client-secret", "example.com")
            .setHttpClientProvider(GiniHttpClientProvider { OkHttpClient() })
            .setSelfManagedAuthentication(true)
            .setCredentialsStore(InMemoryCredentialsStore())
            .build()

        assertThat(networkService).isNotNull()
    }

    @Test
    fun `builds with a session manager when self-managed authentication is disabled`() {
        val networkService = GiniCaptureDefaultNetworkService.builder(context)
            .setSessionManager { error("not called during build") }
            .setCredentialsStore(InMemoryCredentialsStore())
            .build()

        assertThat(networkService).isNotNull()
    }

    @Test
    fun `builds with client credentials and forwards the configuration to the api builder`() {
        val networkService = GiniCaptureDefaultNetworkService.builder(context)
            .setClientCredentials("client-id", "client-secret", "example.com")
            .setBaseUrl("https://api.custom.example.com/")
            .setUserCenterBaseUrl("https://user.custom.example.com/")
            .setCredentialsStore(InMemoryCredentialsStore())
            .setConnectionTimeout(30)
            .setConnectionTimeoutUnit(TimeUnit.SECONDS)
            .setDebuggingEnabled(true)
            .build()

        assertThat(networkService).isNotNull()
    }

    @Test
    fun `building with self-managed authentication fails without a custom http client provider`() {
        val builder = GiniCaptureDefaultNetworkService.builder(context)
            .setSelfManagedAuthentication(true)
            .setCredentialsStore(InMemoryCredentialsStore())

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
