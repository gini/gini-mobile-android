package net.gini.android.capture.fraudSignals

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import org.junit.Test

class GetFraudSignalsEnabledUseCaseTest {

    @Test
    fun `returns false by default`() {
        val useCase = GetFraudSignalsEnabledUseCase(GiniBankConfigurationProvider())

        assertThat(useCase()).isFalse()
    }

    @Test
    fun `returns the flag from the client configuration`() {
        val provider = GiniBankConfigurationProvider().apply {
            update { it.copy(isFraudSignalsEnabled = true) }
        }

        val useCase = GetFraudSignalsEnabledUseCase(provider)

        assertThat(useCase()).isTrue()
    }
}
