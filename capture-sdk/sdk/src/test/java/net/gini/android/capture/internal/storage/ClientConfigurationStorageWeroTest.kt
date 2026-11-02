package net.gini.android.capture.internal.storage

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.gini.android.capture.internal.network.Configuration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Mirrors the androidTest at `src/androidTest/.../ClientConfigurationStorageTest.kt` for the
 * Wero flag only, so Sonar's unit-test JaCoCo picks up the two lines in
 * [ClientConfigurationStorage] that handle `isWeroQRCodeScanningEnabled`.
 */
@RunWith(AndroidJUnit4::class)
class ClientConfigurationStorageWeroTest {

    private lateinit var storage: ClientConfigurationStorage

    @Before
    fun setUp() = runTest {
        storage = ClientConfigurationStorage(ApplicationProvider.getApplicationContext())
        storage.clearConfiguration()
    }

    @Test
    fun `saves and reads the Wero QR scanning flag`() = runTest {
        storage.saveConfiguration(configuration(isWeroQRCodeScanningEnabled = true))

        val result = storage.getConfiguration().first()

        assertThat(result).isNotNull()
        assertThat(result!!.isWeroQRCodeScanningEnabled).isTrue()
    }

    @Test
    fun `defaults the Wero QR scanning flag to false when absent`() = runTest {
        storage.saveConfiguration(configuration(isWeroQRCodeScanningEnabled = false))

        val result = storage.getConfiguration().first()

        assertThat(result).isNotNull()
        assertThat(result!!.isWeroQRCodeScanningEnabled).isFalse()
    }

    private fun configuration(isWeroQRCodeScanningEnabled: Boolean) = Configuration(
        clientID = "test-client-id",
        amplitudeApiKey = "test-amplitude-key",
        isUserJourneyAnalyticsEnabled = false,
        isSkontoEnabled = false,
        isReturnAssistantEnabled = false,
        isTransactionDocsEnabled = false,
        isQrCodeEducationEnabled = false,
        isInstantPaymentEnabled = false,
        isEInvoiceEnabled = false,
        isSavePhotosLocallyEnabled = false,
        isAlreadyPaidHintEnabled = false,
        isPaymentDueHintEnabled = false,
        isUnsupportedQRCodeWarningEnabled = false,
        isPaymentScheduleHintEnabled = false,
        isCreditNoteHintEnabled = false,
        ingredientBrandScreens = emptySet(),
        isWeroQRCodeScanningEnabled = isWeroQRCodeScanningEnabled,
    )
}
