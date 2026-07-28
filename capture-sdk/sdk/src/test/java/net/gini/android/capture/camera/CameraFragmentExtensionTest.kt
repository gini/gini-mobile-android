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
        extension = object : CameraFragmentExtension() {
            override fun hideImageCorners() = Unit
        }
    }

    @After
    fun tearDown() {
        // Restores the production singletons so the test instances don't leak into other test
        // classes sharing the same Koin context.
        getGiniCaptureKoin().unloadModules(listOf(koinTestModule))
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
}
