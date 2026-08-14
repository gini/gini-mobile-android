package net.gini.android.capture.paymentHints

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import org.junit.Test

class GetPaymentScheduleHintEnabledUseCaseTest {

    @Test
    fun `returns false by default`() {
        val useCase = GetPaymentScheduleHintEnabledUseCase(GiniBankConfigurationProvider())

        assertThat(useCase()).isFalse()
    }

    @Test
    fun `returns the flag from the client configuration`() {
        val provider = GiniBankConfigurationProvider().apply {
            update { it.copy(isPaymentScheduleHintEnabled = true) }
        }

        val useCase = GetPaymentScheduleHintEnabledUseCase(provider)

        assertThat(useCase()).isTrue()
    }

    /**
     * The scheduled payment state must not be coupled to the payment due hint flag — see
     * PP-3264 requirement 2.
     */
    @Test
    fun `is independent of the payment due hint flag`() {
        val provider = GiniBankConfigurationProvider().apply {
            update {
                it.copy(
                    isPaymentScheduleHintEnabled = true,
                    isPaymentDueHintEnabled = false
                )
            }
        }

        val useCase = GetPaymentScheduleHintEnabledUseCase(provider)

        assertThat(useCase()).isTrue()
    }
}
