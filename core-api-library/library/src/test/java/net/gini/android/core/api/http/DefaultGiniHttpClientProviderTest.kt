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
    fun `an explicitly configured connection timeout applies to connect only`() {
        val client = DefaultGiniHttpClientProvider.builder(context)
            .setConnectionTimeoutInMs(10_000)
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
    fun `connection and read write timeouts can be configured together`() {
        val client = DefaultGiniHttpClientProvider.builder(context)
            .setConnectionTimeoutInMs(10_000)
            .setReadWriteTimeoutInMs(90_000)
            .build()
            .provideOkHttpClient()

        assertThat(client.connectTimeoutMillis).isEqualTo(10_000)
        assertThat(client.readTimeoutMillis).isEqualTo(90_000)
        assertThat(client.writeTimeoutMillis).isEqualTo(90_000)
    }

    @Test
    fun `a negative read write timeout is rejected`() {
        val builder = DefaultGiniHttpClientProvider.builder(context)

        val exception = runCatching { builder.setReadWriteTimeoutInMs(-1) }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `a negative connection timeout is rejected`() {
        val builder = DefaultGiniHttpClientProvider.builder(context)

        val exception = runCatching { builder.setConnectionTimeoutInMs(-1) }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }
}
