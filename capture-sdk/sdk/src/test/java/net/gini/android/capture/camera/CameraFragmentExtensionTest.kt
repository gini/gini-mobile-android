package net.gini.android.capture.camera

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.education.GetEducationFeatureEnabledUseCase
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.ingredientbrand.IngredientBrandLoadingIndicatorAdapter
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.internal.provider.UnsupportedQrWarningSessionPin
import net.gini.android.capture.internal.qrcode.PaymentQRCodeData
import net.gini.android.capture.internal.qreducation.GetQrEducationTypeUseCase
import net.gini.android.capture.internal.qreducation.IncrementQrCodeRecognizedCounterUseCase
import net.gini.android.capture.internal.qreducation.UpdateFlowTypeUseCase
import net.gini.android.capture.internal.qreducation.model.QrEducationType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

class CameraFragmentExtensionTest {

    /**
     * Records every [setPoweredByGiniVisible] call and exposes the `protected`
     * [isQrEducationStepRunning], so the tests can assert on both.
     */
    private class RecordingExtension(
        private val onlyQRCodeScanningEnabled: () -> Boolean
    ) : CameraFragmentExtension() {

        val poweredByGiniCalls = mutableListOf<Boolean>()

        var interactionBlockedCount = 0

        override fun hideImageCorners() = Unit
        override fun blockInteractionForQrCodeStep() {
            interactionBlockedCount++
        }

        override fun setPoweredByGiniVisible(visible: Boolean) {
            poweredByGiniCalls += visible
        }

        override fun isOnlyQRCodeScanningEnabled() = onlyQRCodeScanningEnabled()

        fun educationStepRunning(): Boolean = isQrEducationStepRunning()
    }

    private lateinit var configurationProvider: GiniBankConfigurationProvider
    private lateinit var sessionPin: UnsupportedQrWarningSessionPin
    private lateinit var qrEducationTypeUseCase: GetQrEducationTypeUseCase
    private lateinit var educationFeatureEnabledUseCase: GetEducationFeatureEnabledUseCase
    private lateinit var koinTestModule: Module
    private lateinit var extension: RecordingExtension
    private var onlyQRCodeScanningEnabled = false

    private val qrCodeData = mockk<PaymentQRCodeData>(relaxed = true)

    @Before
    fun setup() {
        // Fresh instances per test: both are singletons in the SDK's isolated Koin context,
        // which is shared by all tests running in the same JVM.
        configurationProvider = GiniBankConfigurationProvider()
        sessionPin = UnsupportedQrWarningSessionPin()
        // Stubbed rather than real: showQrCodePopup resolves these on the way into both halves,
        // and the production definitions need Android infrastructure a JVM unit test has not got.
        qrEducationTypeUseCase = mockk(relaxed = true)
        educationFeatureEnabledUseCase = mockk(relaxed = true)
        koinTestModule = module {
            single { configurationProvider }
            single { sessionPin }
            single { qrEducationTypeUseCase }
            single { educationFeatureEnabledUseCase }
            single { mockk<UpdateFlowTypeUseCase>(relaxed = true) }
            single { mockk<IncrementQrCodeRecognizedCounterUseCase>(relaxed = true) }
        }
        getGiniCaptureKoin().loadModules(listOf(koinTestModule))
        onlyQRCodeScanningEnabled = false
        extension = RecordingExtension { onlyQRCodeScanningEnabled }
        extension.mPaymentQRCodePopup = mockk(relaxed = true)
        extension.qrCodeEducationPopup = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        // Koin's unloadModules drops the overriding definitions instead of restoring the
        // production ones, so the definitions that have no capture-sdk production binding are
        // re-declared here: otherwise the shared Koin context is left without them and every
        // later test in this JVM that resolves one fails with NoDefinitionFoundException.
        getGiniCaptureKoin().unloadModules(listOf(koinTestModule))
        getGiniCaptureKoin().loadModules(
            listOf(module { single { UnsupportedQrWarningSessionPin() } })
        )
    }

    /** Drives [CameraFragmentExtension.showQrCodePopup] into its invoice-retrieval branch. */
    private fun runRetrievalHalf() {
        coEvery { qrEducationTypeUseCase.execute() } returns null
        every { educationFeatureEnabledUseCase.invoke() } returns false
        extension.showQrCodePopup(qrCodeData) { }
    }

    /**
     * Drives [CameraFragmentExtension.showQrCodePopup] into its QR-code education branch.
     *
     * The stubbed `show` never runs its completion callback, which is what the real overlay does
     * only after its animation. That is deliberate and does not block: the education mutex starts
     * unlocked, so the `lock()` straight after `show` acquires it and returns immediately. Each
     * test builds a fresh extension, so the mutex left locked here never leaks into another one.
     */
    private fun runEducationHalf() {
        coEvery { qrEducationTypeUseCase.execute() } returns QrEducationType.PHOTO_DOC
        every { educationFeatureEnabledUseCase.invoke() } returns true
        extension.showQrCodePopup(qrCodeData) { }
    }

    /**
     * The approved design shows the brand element from the QR-detected state on, not only once
     * the invoice starts loading, so the retrieval half raises it when its popup appears.
     *
     * R13 still holds because the controls are put out of reach first — asserted here rather
     * than left implicit, since raising the badge over a live shutter is the exact thing that
     * rule forbids.
     */
    @Test
    fun `retrieval half raises the brand element once the controls are out of reach`() {
        configurationProvider.update { it.copy(ingredientBrandScreens = setOf("Analysis")) }

        runRetrievalHalf()

        assertThat(extension.poweredByGiniCalls).containsExactly(true)
        assertThat(extension.interactionBlockedCount).isEqualTo(1)
    }

    @Test
    fun `retrieval half leaves the brand element alone when the configuration does not list the screen`() {
        runRetrievalHalf()

        assertThat(extension.poweredByGiniCalls).isEmpty()
        // Still blocked: the step covers the preview whether or not the badge is enabled.
        assertThat(extension.interactionBlockedCount).isEqualTo(1)
    }

    /**
     * Starts with the education half so the flag is genuinely `true` going in — otherwise this
     * would pass on the initial `false` alone and would not notice the reset at the top of
     * `showQrCodePopup` being removed.
     */
    @Test
    fun `retrieval half does not leave the education step marked as running`() {
        runEducationHalf()
        assertThat(extension.educationStepRunning()).isTrue()

        runRetrievalHalf()

        assertThat(extension.educationStepRunning()).isFalse()
    }

    @Test
    fun `education half raises the brand element when the configuration lists the screen`() {
        configurationProvider.update { it.copy(ingredientBrandScreens = setOf("Analysis")) }

        runEducationHalf()

        assertThat(extension.poweredByGiniCalls).containsExactly(true)
        assertThat(extension.educationStepRunning()).isTrue()
    }

    @Test
    fun `education half leaves the brand element alone when the configuration does not list the screen`() {
        runEducationHalf()

        assertThat(extension.poweredByGiniCalls).isEmpty()
        assertThat(extension.educationStepRunning()).isTrue()
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

    private class NoopIndicatorAdapter : CustomLoadingIndicatorAdapter {
        override fun onCreateView(container: ViewGroup): View = View(container.context)
        override fun onVisible() = Unit
        override fun onHidden() = Unit
        override fun onDestroy() = Unit
    }

    /**
     * The camera always binds its own adapter, which chooses between the Gini mark and the
     * integrator's indicator when the indicator is shown. It cannot choose here: this is the first
     * screen the SDK opens and ingredientBrandScreens has not arrived yet.
     */
    @Test
    fun `always provides the camera loading indicator adapter`() {
        val instance = extension.loadingIndicatorAdapterInstance(NoopIndicatorAdapter())

        assertThat(instance.viewAdapter)
            .isInstanceOf(IngredientBrandLoadingIndicatorAdapter::class.java)
    }

    /**
     * InjectedViewContainer tracks adapter ownership per instance, so the camera must hand back the
     * same one across view recreations rather than a fresh instance each time.
     */
    @Test
    fun `reuses the same loading indicator instance across view recreations`() {
        val first = extension.loadingIndicatorAdapterInstance(NoopIndicatorAdapter())
        val second = extension.loadingIndicatorAdapterInstance(NoopIndicatorAdapter())

        assertThat(second).isSameInstanceAs(first)
    }
}
