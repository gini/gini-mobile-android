package net.gini.android.capture

import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import net.gini.android.capture.internal.network.Configuration
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.internal.provider.UnsupportedQrWarningSessionPin
import net.gini.android.capture.internal.storage.ClientConfigurationStorage
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GiniCaptureViewModelTest {

    private val persistedConfiguration = MutableStateFlow<Configuration?>(null)
    private val clientConfigurationStorage = mockk<ClientConfigurationStorage> {
        every { getConfiguration() } returns persistedConfiguration
    }
    private val configurationProvider = GiniBankConfigurationProvider()
    private val sessionPin = UnsupportedQrWarningSessionPin()

    @Before
    fun setup() {
        // The DataStore observer launches in viewModelScope, which dispatches on Main.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `provider tracks the fresh configuration instead of latching the stale first emission`() {
        createViewModel()

        // Given: the first DataStore emission is the configuration cached by the previous
        // session, in which the new unsupported-QR warning was still enabled
        persistedConfiguration.value = configuration(isUnsupportedQRCodeWarningEnabled = true)

        // When: the fresh remote configuration of this session disables the warning
        persistedConfiguration.value = configuration(isUnsupportedQRCodeWarningEnabled = false)

        // Then: the provider reflects the fresh value — the observer must not pin the stale one
        assertThat(configurationProvider.provide().isUnsupportedQRCodeWarningEnabled).isFalse()

        // And: the first warning shown in this session pins the fresh value, not the stale one
        val pinnedAtFirstWarning = sessionPin.pinIfAbsent {
            configurationProvider.provide().isUnsupportedQRCodeWarningEnabled
        }
        assertThat(pinnedAtFirstWarning).isFalse()
    }

    @Test
    fun `observer preserves the clientID and amplitudeApiKey already held by the provider`() {
        // Given: the configuration API already delivered the non-persisted values
        configurationProvider.update {
            it.copy(clientID = "client-id", amplitudeApiKey = "amplitude-key")
        }
        createViewModel()

        // When: DataStore emits a persisted configuration, which never carries them
        persistedConfiguration.value = configuration(isUnsupportedQRCodeWarningEnabled = true)

        // Then: the persisted flags are taken over while the in-memory values survive
        val provided = configurationProvider.provide()
        assertThat(provided.clientID).isEqualTo("client-id")
        assertThat(provided.amplitudeApiKey).isEqualTo("amplitude-key")
        assertThat(provided.isUnsupportedQRCodeWarningEnabled).isTrue()
    }

    @Test
    fun `clearing the ViewModel releases the pinned warning type for the next session`() {
        val viewModelStore = ViewModelStore()
        viewModelStore.put("gini-capture", createViewModel())

        // Given: the old yellow warning was pinned when the first warning was shown, and the
        // configuration enabled the new warning afterwards
        assertThat(sessionPin.pinIfAbsent { false }).isFalse()
        persistedConfiguration.value = configuration(isUnsupportedQRCodeWarningEnabled = true)
        assertThat(sessionPin.pinIfAbsent { true }).isFalse()

        // When: the session ends (onCleared is called)
        viewModelStore.clear()

        // Then: the next session pins the fresh configuration value
        val pinnedInNextSession = sessionPin.pinIfAbsent {
            configurationProvider.provide().isUnsupportedQRCodeWarningEnabled
        }
        assertThat(pinnedInNextSession).isTrue()
    }

    private fun createViewModel() = GiniCaptureViewModel(
        clientConfigurationStorage = clientConfigurationStorage,
        giniBankConfigurationProvider = configurationProvider,
        unsupportedQrWarningSessionPin = sessionPin,
    )

    private fun configuration(isUnsupportedQRCodeWarningEnabled: Boolean) = Configuration(
        clientID = "",
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
        isUnsupportedQRCodeWarningEnabled = isUnsupportedQRCodeWarningEnabled,
    )
}
