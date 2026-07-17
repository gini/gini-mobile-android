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
