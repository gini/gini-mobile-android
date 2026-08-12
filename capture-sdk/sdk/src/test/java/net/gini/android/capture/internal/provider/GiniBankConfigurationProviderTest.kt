package net.gini.android.capture.internal.provider

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GiniBankConfigurationProviderTest {

    @Test
    fun `update applies the transform to the current configuration`() {
        val provider = GiniBankConfigurationProvider()

        provider.update { it.copy(clientID = "client-1") }
        provider.update { it.copy(isSkontoEnabled = true) }

        assertThat(provider.provide().clientID).isEqualTo("client-1")
        assertThat(provider.provide().isSkontoEnabled).isTrue()
    }

    @Test
    fun `concurrent updates do not lose each other's fields`() {
        // Regression test for the read-copy-write race between the network callback (background
        // thread, sets clientID/amplitudeApiKey) and the DataStore observer (main thread, sets
        // the boolean flags): with non-atomic updates one side's write could be lost entirely.
        repeat(100) {
            val provider = GiniBankConfigurationProvider()
            val executor = Executors.newFixedThreadPool(2)
            val start = CountDownLatch(1)
            try {
                val networkUpdate = executor.submit {
                    start.await()
                    provider.update {
                        it.copy(clientID = "client-id", amplitudeApiKey = "api-key")
                    }
                }
                val observerUpdate = executor.submit {
                    start.await()
                    provider.update {
                        it.copy(isUnsupportedQRCodeWarningEnabled = true, isSkontoEnabled = true)
                    }
                }
                start.countDown()
                networkUpdate.get(5, TimeUnit.SECONDS)
                observerUpdate.get(5, TimeUnit.SECONDS)

                val result = provider.provide()
                assertThat(result.clientID).isEqualTo("client-id")
                assertThat(result.amplitudeApiKey).isEqualTo("api-key")
                assertThat(result.isUnsupportedQRCodeWarningEnabled).isTrue()
                assertThat(result.isSkontoEnabled).isTrue()
            } finally {
                executor.shutdown()
            }
        }
    }
}
