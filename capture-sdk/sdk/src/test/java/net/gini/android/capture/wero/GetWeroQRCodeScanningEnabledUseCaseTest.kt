package net.gini.android.capture.wero

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import org.junit.Test

class GetWeroQRCodeScanningEnabledUseCaseTest {

    /**
     * The backend omitted the flag, so the provider still holds its default configuration.
     */
    @Test
    fun `returns false by default`() {
        val useCase = GetWeroQRCodeScanningEnabledUseCase(GiniBankConfigurationProvider())

        assertThat(useCase()).isFalse()
    }

    @Test
    fun `returns the flag from the client configuration`() {
        val provider = GiniBankConfigurationProvider().apply {
            update { it.copy(isWeroQRCodeScanningEnabled = true) }
        }

        val useCase = GetWeroQRCodeScanningEnabledUseCase(provider)

        assertThat(useCase()).isTrue()
    }
}
