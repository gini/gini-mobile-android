package net.gini.android.core.api.http

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the timeout configuration of the OkHttp client created by [DefaultGiniHttpClientProvider].
 */
@RunWith(AndroidJUnit4::class)
class DefaultGiniHttpClientProviderTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `the default client bounds the connect timeout separately from the read and write timeouts`() {
        // On a broken dual-stack network OkHttp 4 tries the IPv6 address first and only falls back to
        // IPv4 after the connect attempt times out - the connect timeout must therefore be short,
        // while read and write keep the long timeout needed for large document uploads.
        val client = DefaultGiniHttpClientProvider.builder(context).build().provideOkHttpClient()

        assertThat(client.connectTimeoutMillis).isEqualTo(15_000)
        assertThat(client.readTimeoutMillis).isEqualTo(60_000)
        assertThat(client.writeTimeoutMillis).isEqualTo(60_000)
    }

    @Test
    fun `an explicitly configured connect timeout applies to connect only`() {
        val client = DefaultGiniHttpClientProvider.builder(context)
            .setConnectTimeoutInMs(10_000)
            .build()
            .provideOkHttpClient()

        assertThat(client.connectTimeoutMillis).isEqualTo(10_000)
        assertThat(client.readTimeoutMillis).isEqualTo(60_000)
        assertThat(client.writeTimeoutMillis).isEqualTo(60_000)
    }

    @Test
    fun `an explicitly configured read write timeout leaves the connect timeout at its default`() {
        val client = DefaultGiniHttpClientProvider.builder(context)
            .setReadWriteTimeoutInMs(90_000)
            .build()
            .provideOkHttpClient()

        assertThat(client.connectTimeoutMillis).isEqualTo(15_000)
        assertThat(client.readTimeoutMillis).isEqualTo(90_000)
        assertThat(client.writeTimeoutMillis).isEqualTo(90_000)
    }

    @Test
    fun `connect and read write timeouts can be configured together`() {
        val client = DefaultGiniHttpClientProvider.builder(context)
            .setConnectTimeoutInMs(10_000)
            .setReadWriteTimeoutInMs(90_000)
            .build()
            .provideOkHttpClient()

        assertThat(client.connectTimeoutMillis).isEqualTo(10_000)
        assertThat(client.readTimeoutMillis).isEqualTo(90_000)
        assertThat(client.writeTimeoutMillis).isEqualTo(90_000)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `the deprecated connection timeout still applies to connect read and write`() {
        // Integrators who call the old setter keep exactly the behaviour they had: one value for all three
        val client = DefaultGiniHttpClientProvider.builder(context)
            .setConnectionTimeoutInMs(10_000)
            .build()
            .provideOkHttpClient()

        assertThat(client.connectTimeoutMillis).isEqualTo(10_000)
        assertThat(client.readTimeoutMillis).isEqualTo(10_000)
        assertThat(client.writeTimeoutMillis).isEqualTo(10_000)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `the dedicated timeout setters take precedence over the deprecated connection timeout`() {
        // Regardless of call order: the deprecated setter is called last here
        val client = DefaultGiniHttpClientProvider.builder(context)
            .setConnectTimeoutInMs(5_000)
            .setReadWriteTimeoutInMs(90_000)
            .setConnectionTimeoutInMs(30_000)
            .build()
            .provideOkHttpClient()

        assertThat(client.connectTimeoutMillis).isEqualTo(5_000)
        assertThat(client.readTimeoutMillis).isEqualTo(90_000)
        assertThat(client.writeTimeoutMillis).isEqualTo(90_000)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `the deprecated connection timeout fills in the timeout the dedicated setters left unset`() {
        val client = DefaultGiniHttpClientProvider.builder(context)
            .setConnectionTimeoutInMs(30_000)
            .setReadWriteTimeoutInMs(90_000)
            .build()
            .provideOkHttpClient()

        assertThat(client.connectTimeoutMillis).isEqualTo(30_000)
        assertThat(client.readTimeoutMillis).isEqualTo(90_000)
        assertThat(client.writeTimeoutMillis).isEqualTo(90_000)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `negative timeouts are rejected by all three timeout setters`() {
        val builder = DefaultGiniHttpClientProvider.builder(context)

        assertThat(runCatching { builder.setConnectTimeoutInMs(-1) }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(runCatching { builder.setReadWriteTimeoutInMs(-1) }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(runCatching { builder.setConnectionTimeoutInMs(-1) }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
