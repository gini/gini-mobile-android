package net.gini.android.capture.camera

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.internal.provider.UnsupportedQrWarningSessionPin
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

class CameraFragmentExtensionTest {

    private lateinit var configurationProvider: GiniBankConfigurationProvider
    private lateinit var sessionPin: UnsupportedQrWarningSessionPin
    private lateinit var koinTestModule: Module
    private lateinit var extension: CameraFragmentExtension
    private var onlyQRCodeScanningEnabled = false

    @Before
    fun setup() {
        // Fresh instances per test: both are singletons in the SDK's isolated Koin context,
        // which is shared by all tests running in the same JVM.
        configurationProvider = GiniBankConfigurationProvider()
        sessionPin = UnsupportedQrWarningSessionPin()
        koinTestModule = module {
            single { configurationProvider }
            single { sessionPin }
        }
        getGiniCaptureKoin().loadModules(listOf(koinTestModule))
        onlyQRCodeScanningEnabled = false
        extension = object : CameraFragmentExtension() {
            override fun hideImageCorners() = Unit
            override fun isOnlyQRCodeScanningEnabled() = onlyQRCodeScanningEnabled
        }
    }

    @After
    fun tearDown() {
        // Koin's unloadModules drops the overriding definitions instead of restoring the
        // production ones, so the session pin is re-declared here: otherwise the shared Koin
        // context is left with no UnsupportedQrWarningSessionPin definition and every later
        // test in this JVM that resolves it fails with NoDefinitionFoundException.
        getGiniCaptureKoin().unloadModules(listOf(koinTestModule))
        getGiniCaptureKoin().loadModules(
            listOf(module { single { UnsupportedQrWarningSessionPin() } })
        )
    }

    @Test
    fun `second unsupported QR scan shows the same warning type even if the configuration changed in between`() {
        // Given: the configuration endpoint has not responded when the first invalid QR code is
        // scanned, so the default (old yellow warning) is pinned for the session
        assertThat(extension.isUnsupportedQRCodeWarningEnabled()).isFalse()

        // When: the configuration endpoint resolves in between and enables the new warning
        configurationProvider.update { it.copy(isUnsupportedQRCodeWarningEnabled = true) }

        // Then: a second scan in the same session still shows the old yellow warning
        assertThat(extension.isUnsupportedQRCodeWarningEnabled()).isFalse()
    }

    @Test
    fun `configuration arriving before the first scan decides the warning type for the session`() {
        // Given: the configuration was loaded before any invalid QR code was scanned
        configurationProvider.update { it.copy(isUnsupportedQRCodeWarningEnabled = true) }

        // When/Then: the first scan pins the loaded value
        assertThat(extension.isUnsupportedQRCodeWarningEnabled()).isTrue()

        // And: a configuration flip after the first warning does not change the running session
        configurationProvider.update { it.copy(isUnsupportedQRCodeWarningEnabled = false) }
        assertThat(extension.isUnsupportedQRCodeWarningEnabled()).isTrue()
    }

    @Test
    fun `configuration change applies from the next session onward`() {
        // Given: the old yellow warning was pinned in this session, then the configuration
        // enabled the new warning
        assertThat(extension.isUnsupportedQRCodeWarningEnabled()).isFalse()
        configurationProvider.update { it.copy(isUnsupportedQRCodeWarningEnabled = true) }

        // When: the session ends (GiniCaptureViewModel.onCleared releases the pin)
        sessionPin.reset()

        // Then: the next session picks up the changed configuration
        assertThat(extension.isUnsupportedQRCodeWarningEnabled()).isTrue()
    }

    @Test
    fun `QR-only mode pins the old yellow warning even when the new warning is enabled`() {
        // Given: the new warning is enabled, but only QR code scanning is configured, so the
        // dialog's "Take photo of document" action would be invalid
        configurationProvider.update { it.copy(isUnsupportedQRCodeWarningEnabled = true) }
        onlyQRCodeScanningEnabled = true

        // When/Then: the first scan pins the old yellow warning
        assertThat(extension.isUnsupportedQRCodeWarningEnabled()).isFalse()

        // And: the pin holds for the whole session
        assertThat(extension.isUnsupportedQRCodeWarningEnabled()).isFalse()
    }

    @Test
    fun `switching to QR-only mode via the dialog keeps the new warning for the session`() {
        // Given: the new warning was pinned while document capture was still available
        configurationProvider.update { it.copy(isUnsupportedQRCodeWarningEnabled = true) }
        assertThat(extension.isUnsupportedQRCodeWarningEnabled()).isTrue()

        // When: the user picks "Only QR scanning" in the dialog (runtime mode switch)
        onlyQRCodeScanningEnabled = true

        // Then: only one warning type per session — the next scan still shows the new dialog
        assertThat(extension.isUnsupportedQRCodeWarningEnabled()).isTrue()
    }

    @Test
    fun `QR-only mode decided at the first scan holds for the whole session`() {
        // Given: the old yellow warning was pinned because of QR-only mode
        configurationProvider.update { it.copy(isUnsupportedQRCodeWarningEnabled = true) }
        onlyQRCodeScanningEnabled = true
        assertThat(extension.isUnsupportedQRCodeWarningEnabled()).isFalse()

        // When: the session ends and the next one starts without QR-only mode
        sessionPin.reset()
        onlyQRCodeScanningEnabled = false

        // Then: the next session pins the new warning again
        assertThat(extension.isUnsupportedQRCodeWarningEnabled()).isTrue()
    }
}
