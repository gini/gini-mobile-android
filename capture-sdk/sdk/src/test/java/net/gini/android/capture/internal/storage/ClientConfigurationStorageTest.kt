package net.gini.android.capture.internal.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.gini.android.capture.internal.network.Configuration
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClientConfigurationStorageTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val storage = ClientConfigurationStorage(context)

    @Before
    fun setUp() = runTest {
        storage.clearConfiguration()
    }

    @After
    fun tearDown() = runTest {
        storage.clearConfiguration()
    }

    @Test
    fun `fraud signals flag round-trips through storage`() = runTest {
        storage.saveConfiguration(configuration(isFraudSignalsEnabled = true))

        assertThat(storage.getConfiguration().first()?.isFraudSignalsEnabled).isTrue()
    }

    @Test
    fun `cache from an older SDK without the fraud signals key reads the flag as false`() = runTest {
        storage.saveConfiguration(configuration(isFraudSignalsEnabled = true))
        context.dataStore.edit { it.remove(booleanPreferencesKey("is_fraud_signals_enabled")) }

        val stored = storage.getConfiguration().first()

        assertThat(stored).isNotNull()
        assertThat(stored!!.isFraudSignalsEnabled).isFalse()
    }

    private fun configuration(isFraudSignalsEnabled: Boolean) = Configuration(
        clientID = "client",
        isUserJourneyAnalyticsEnabled = false,
        isSkontoEnabled = false,
        isReturnAssistantEnabled = false,
        isTransactionDocsEnabled = false,
        isQrCodeEducationEnabled = false,
        isInstantPaymentEnabled = false,
        isEInvoiceEnabled = false,
        amplitudeApiKey = "",
        isSavePhotosLocallyEnabled = false,
        isAlreadyPaidHintEnabled = false,
        isPaymentDueHintEnabled = false,
        isFraudSignalsEnabled = isFraudSignalsEnabled,
    )
}
