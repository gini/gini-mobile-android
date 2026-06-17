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
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ClientConfigurationStorageTest {

    private lateinit var storage: ClientConfigurationStorage

    @Before
    fun setUp() = runTest {
        storage = ClientConfigurationStorage(ApplicationProvider.getApplicationContext())
        storage.clearConfiguration()
    }

    @Test
    fun getConfiguration_returnsNull_whenNothingIsSaved() = runTest {
        val result = storage.getConfiguration().first()
        assertThat(result).isNull()
    }

    @Test
    fun getConfiguration_returnsSavedConfiguration() = runTest {
        val configuration = buildConfiguration(
            isSkontoEnabled = true,
            isUnsupportedQRCodeWarningEnabled = true,
            isEInvoiceEnabled = true,
        )

        storage.saveConfiguration(configuration)

        val result = storage.getConfiguration().first()
        assertThat(result).isNotNull()
        assertThat(result!!.isUserJourneyAnalyticsEnabled).isEqualTo(configuration.isUserJourneyAnalyticsEnabled)
        assertThat(result.isSkontoEnabled).isTrue()
        assertThat(result.isReturnAssistantEnabled).isEqualTo(configuration.isReturnAssistantEnabled)
        assertThat(result.isTransactionDocsEnabled).isEqualTo(configuration.isTransactionDocsEnabled)
        assertThat(result.isQrCodeEducationEnabled).isEqualTo(configuration.isQrCodeEducationEnabled)
        assertThat(result.isInstantPaymentEnabled).isEqualTo(configuration.isInstantPaymentEnabled)
        assertThat(result.isEInvoiceEnabled).isTrue()
        assertThat(result.isSavePhotosLocallyEnabled).isEqualTo(configuration.isSavePhotosLocallyEnabled)
        assertThat(result.isAlreadyPaidHintEnabled).isEqualTo(configuration.isAlreadyPaidHintEnabled)
        assertThat(result.isPaymentDueHintEnabled).isEqualTo(configuration.isPaymentDueHintEnabled)
        assertThat(result.isUnsupportedQRCodeWarningEnabled).isTrue()
    }

    @Test
    fun getConfiguration_returnsLatestSavedConfiguration() = runTest {
        storage.saveConfiguration(buildConfiguration(isSkontoEnabled = true))
        storage.saveConfiguration(buildConfiguration(isSkontoEnabled = false))

        val result = storage.getConfiguration().first()

        assertThat(result!!.isSkontoEnabled).isFalse()
    }

    private fun buildConfiguration(
        isSkontoEnabled: Boolean = false,
        isUnsupportedQRCodeWarningEnabled: Boolean = false,
        isEInvoiceEnabled: Boolean = false,
    ) = Configuration(
        id = UUID.randomUUID(),
        clientID = "test-client-id",
        amplitudeApiKey = "test-amplitude-key",
        isUserJourneyAnalyticsEnabled = false,
        isSkontoEnabled = isSkontoEnabled,
        isReturnAssistantEnabled = false,
        isTransactionDocsEnabled = false,
        isQrCodeEducationEnabled = false,
        isInstantPaymentEnabled = false,
        isEInvoiceEnabled = isEInvoiceEnabled,
        isSavePhotosLocallyEnabled = false,
        isAlreadyPaidHintEnabled = false,
        isPaymentDueHintEnabled = false,
        isUnsupportedQRCodeWarningEnabled = isUnsupportedQRCodeWarningEnabled,
    )
}
