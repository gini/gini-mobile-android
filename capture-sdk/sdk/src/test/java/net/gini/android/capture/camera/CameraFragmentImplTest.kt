package net.gini.android.capture.camera

import android.app.Activity
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.view.ViewGroup
import com.nhaarman.mockitokotlin2.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import jersey.repackaged.jsr166e.CompletableFuture
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.di.clientConfigurationModule
import net.gini.android.capture.di.educationModule
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.di.qrEducationModule
import net.gini.android.capture.document.QRCodeDocument
import net.gini.android.capture.education.GetEducationFeatureEnabledUseCase
import net.gini.android.capture.internal.camera.api.CameraInterface
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.internal.provider.UnsupportedQrWarningSessionPin
import net.gini.android.capture.internal.qrcode.PaymentQRCodeData
import net.gini.android.capture.internal.qreducation.GetQrEducationTypeUseCase
import net.gini.android.capture.internal.qreducation.IncrementQrCodeRecognizedCounterUseCase
import net.gini.android.capture.internal.qreducation.UpdateFlowTypeUseCase
import net.gini.android.capture.internal.qreducation.model.QrEducationType
import net.gini.android.capture.internal.ui.FragmentImplCallback
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.network.GiniCaptureNetworkService
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import net.gini.android.capture.view.InjectedViewContainer
import net.gini.android.capture.tracking.CameraScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.module.Module
import org.koin.dsl.module
import org.robolectric.Robolectric

/**
 * Created by Alpar Szotyori on 02.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class CameraFragmentImplTest {

    private lateinit var qrEducationTypeUseCase: GetQrEducationTypeUseCase
    private lateinit var educationFeatureEnabledUseCase: GetEducationFeatureEnabledUseCase
    private lateinit var koinTestModule: Module

    @Before
    fun setup() {
        // Same overrides as CameraFragmentExtensionTest: showQrCodePopup resolves these on the way
        // into both halves of the QR-code step, and the production definitions need Android
        // infrastructure a JVM unit test has not got.
        qrEducationTypeUseCase = mockk(relaxed = true)
        educationFeatureEnabledUseCase = mockk(relaxed = true)
        koinTestModule = module {
            single { GiniBankConfigurationProvider() }
            single { UnsupportedQrWarningSessionPin() }
            single { qrEducationTypeUseCase }
            single { educationFeatureEnabledUseCase }
            single { mockk<UpdateFlowTypeUseCase>(relaxed = true) }
            single { mockk<IncrementQrCodeRecognizedCounterUseCase>(relaxed = true) }
        }
        getGiniCaptureKoin().loadModules(listOf(koinTestModule))
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
        // unloadModules drops the overriding definitions without restoring the production ones.
        // This class runs in Robolectric's sandbox, whose Koin context is shared with every other
        // Robolectric test class in this JVM (CameraFragmentTest resolves UpdateFlowTypeUseCase in
        // onStart), so the production modules the test module overrode are loaded back.
        getGiniCaptureKoin().unloadModules(listOf(koinTestModule))
        getGiniCaptureKoin().loadModules(
            listOf(educationModule, qrEducationModule, clientConfigurationModule)
        )
    }

    @Test
    fun `triggers Take Picture event`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        val fragmentImpl = object : CameraFragmentImplWithoutQRCodeReader(mock(), mock<CancelListener>(), false) {
            override fun createCameraController(activity: Activity?): CameraInterface {
                return mock<CameraInterface>().apply {
                    whenever(isPreviewRunning).thenReturn(true)
                    whenever(takePicture()).thenReturn(CompletableFuture.completedFuture(mock()))
                }
            }
        }
        fragmentImpl.initCameraController(mock())

        // When
        fragmentImpl.onCameraTriggerClicked()

        // Then
        verify(eventTracker).onCameraScreenEvent(Event(CameraScreenEvent.TAKE_PICTURE))
    }

    @Test
    fun `triggers Help event when help was started`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // Stub the fragment transaction related calls
        val fragmentCallbackStub = mock<FragmentImplCallback>()
        whenever(fragmentCallbackStub.childFragmentManager).thenReturn(object: FragmentManager() {
            override fun beginTransaction(): FragmentTransaction {
                return object: FragmentTransaction() {
                    override fun add(containerViewId: Int, fragment: Fragment, tag: String?): FragmentTransaction {
                        return this;
                    }

                    override fun addToBackStack(name: String?): FragmentTransaction {
                        return this
                    }

                    override fun commit(): Int {
                        return 0
                    }

                    override fun commitAllowingStateLoss(): Int {
                        return 0
                    }

                    override fun commitNow() {
                    }

                    override fun commitNowAllowingStateLoss() {
                    }
                }
            }
        })
        whenever(fragmentCallbackStub.findNavController()).thenReturn(mock())

        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(fragmentCallbackStub, mock(), false)

        val noPermissionLayoutMock = mock<ConstraintLayout> {
            on { visibility } doReturn View.INVISIBLE
        }
        val analyticsTrackerMock = mock<UserAnalyticsEventTracker> {
            on { trackEvent(any()) }.thenReturn(true)
            on { trackEvent(any(), any()) }.thenReturn(true)
        }

        fragmentImpl.mLayoutNoPermission = noPermissionLayoutMock
        fragmentImpl.mUserAnalyticsEventTracker = analyticsTrackerMock

        // When
        fragmentImpl.startHelpActivity()

        // Then
        verify(eventTracker).onCameraScreenEvent(Event(CameraScreenEvent.HELP))
    }

    @Test
    fun `does not reset camera frame color when no IBANs are detected while unsupported QR popup is shown`() {
        // Given: the unsupported QR code popup is shown (e.g. restored after a rotation)
        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(mock(), mock<CancelListener>(), false)
        fragmentImpl.mUnsupportedQRCodePopup = mock { on { isShown } doReturn true }
        fragmentImpl.mImageFrame = mock()
        fragmentImpl.mIbanDetectedTextView = mock()

        // When: a camera frame without IBANs is processed
        fragmentImpl.handleIBANsDetected(emptyList())

        // Then: the frame color set by the popup is left untouched, but the IBAN label is hidden
        verify(fragmentImpl.mImageFrame, never()).setImageTintList(any())
        verify(fragmentImpl.mIbanDetectedTextView).visibility = View.GONE
    }

    @Test
    fun `resets camera frame color when no IBANs are detected and no QR popup is shown`() {
        // Given: no QR code popup is shown
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
        val fragmentCallback = mock<FragmentImplCallback> {
            on { activity } doReturn activityController.get()
        }
        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(fragmentCallback, mock<CancelListener>(), false)
        fragmentImpl.mUnsupportedQRCodePopup = mock { on { isShown } doReturn false }
        fragmentImpl.mPaymentQRCodePopup = mock { on { isShown } doReturn false }
        fragmentImpl.mImageFrame = mock()
        fragmentImpl.mIbanDetectedTextView = mock()

        // When: a camera frame without IBANs is processed
        fragmentImpl.handleIBANsDetected(emptyList())

        // Then: the frame color is reset to the default and the IBAN label is hidden
        verify(fragmentImpl.mImageFrame).setImageTintList(any())
        verify(fragmentImpl.mIbanDetectedTextView).visibility = View.GONE
    }

    @Test
    fun `does not dismiss unsupported QR dialog when IBANs are detected while it is shown`() {
        // Given: the unsupported QR code dialog (new warning) is visible
        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(mock(), mock<CancelListener>(), false)
        fragmentImpl.mIsUnsupportedQRDialogShowing = true
        fragmentImpl.mUnsupportedQRCodePopup = mock { on { isShown } doReturn true }
        fragmentImpl.mPaymentQRCodePopup = mock { on { isShown } doReturn false }
        fragmentImpl.mImageFrame = mock()
        fragmentImpl.mIbanDetectedTextView = mock()

        // When: an in-flight recognition result with IBANs arrives
        fragmentImpl.handleIBANsDetected(listOf("DE75 5121 0800 1245 1261 99"))

        // Then: the dialog stays visible and the IBAN overlay is not drawn over it
        verify(fragmentImpl.mUnsupportedQRCodePopup, never()).hide()
        verify(fragmentImpl.mIbanDetectedTextView, never()).visibility = View.VISIBLE
        verify(fragmentImpl.mImageFrame, never()).setImageTintList(any())
    }

    @Test
    fun `dismisses unsupported QR popup when IBANs are detected while legacy warning is shown`() {
        // Given: the legacy unsupported QR banner is visible (no dialog)
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
        val fragmentCallback = mock<FragmentImplCallback> {
            on { activity } doReturn activityController.get()
        }
        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(fragmentCallback, mock<CancelListener>(), false)
        fragmentImpl.mIsUnsupportedQRDialogShowing = false
        fragmentImpl.mUnsupportedQRCodePopup = mock { on { isShown } doReturn true }
        fragmentImpl.mPaymentQRCodePopup = mock { on { isShown } doReturn false }
        fragmentImpl.mImageFrame = mock()
        fragmentImpl.mIbanDetectedTextView = mock()

        // When: IBANs are detected
        fragmentImpl.handleIBANsDetected(listOf("DE75 5121 0800 1245 1261 99"))

        // Then: the banner is hidden and the IBAN overlay is shown (pre-existing behavior)
        verify(fragmentImpl.mUnsupportedQRCodePopup).hide()
        verify(fragmentImpl.mIbanDetectedTextView).visibility = View.VISIBLE
    }

    @Test
    fun `resets QR code state when unsupported popup hides so scanning resumes`() {
        // Given: state is set as if an unsupported QR code was shown and blocked further scans
        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(mock(), mock<CancelListener>(), false)
        fragmentImpl.mQRCodeContent = "unsupported-qr-content"
        fragmentImpl.mInterfaceHidden = true

        // When: the unsupported QR popup hides
        fragmentImpl.onUnsupportedQRCodePopupHidden()

        // Then: state is reset so subsequent QR codes can be detected
        assertNull(fragmentImpl.mQRCodeContent)
        assertFalse(fragmentImpl.mInterfaceHidden)
    }

    @Test
    fun `enableDocumentCapture does not override integrator-configured QR-only mode`() {
        // Given: the integrator configured QR-code-scanning-only mode
        GiniCapture.Builder()
            .setQRCodeScanningEnabled(true)
            .setOnlyQRCodeScanning(true)
            .build()
        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(mock(), mock<CancelListener>(), false)

        // When: document capture is requested (e.g. through a future caller)
        fragmentImpl.enableDocumentCapture()

        // Then: the runtime override is not set and QR-only mode stays active
        assertNull(fragmentImpl.mOnlyQRCodeScanningRuntimeOverride)
        assertTrue(fragmentImpl.isOnlyQRCodeScanningEnabledForTest())
    }

    @Test
    fun `enableDocumentCapture switches back to document capture in standard mode`() {
        // Given: standard mode, where the user switched to QR-only via the warning dialog
        GiniCapture.Builder()
            .setQRCodeScanningEnabled(true)
            .build()
        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(mock(), mock<CancelListener>(), false)
        fragmentImpl.mOnlyQRCodeScanningRuntimeOverride = true

        // When: the user switches back to document capture
        fragmentImpl.enableDocumentCapture()

        // Then: the runtime override disables QR-only mode again
        assertEquals(false, fragmentImpl.mOnlyQRCodeScanningRuntimeOverride)
        assertFalse(fragmentImpl.isOnlyQRCodeScanningEnabledForTest())
        assertTrue(fragmentImpl.mQRCodeScanningDisabledByUser)
    }

    @Test
    fun `showActivityIndicatorAndDisableInteraction shows the dim and the loading indicator`() {
        // Given: an injected loading indicator the camera owns
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).get()
        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(mock(), mock<CancelListener>(), false)
        val adapter = RecordingLoadingIndicatorAdapter()
        fragmentImpl.mActivityIndicatorBackground = View(activity).apply { visibility = View.INVISIBLE }
        fragmentImpl.mLoadingIndicator = loadingIndicatorOwning(adapter)

        // When: the public, unconditional variant is used (every busy state but the QR step)
        fragmentImpl.showActivityIndicatorAndDisableInteraction()

        // Then: the dim swallows touches and the indicator is shown
        assertEquals(View.VISIBLE, fragmentImpl.mActivityIndicatorBackground.visibility)
        assertTrue(fragmentImpl.mActivityIndicatorBackground.isClickable)
        assertEquals(1, adapter.visibleCalls)
    }

    @Test
    fun `analyzeQRCode shows the loading indicator on the invoice-retrieval half`() {
        // Given: a network service, so the QR code can be uploaded, and no education on screen
        GiniCapture.Builder().setGiniCaptureNetworkService(mock<GiniCaptureNetworkService>()).build()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).get()
        val fragmentImpl = qrCodeFragmentImpl(activity)
        val adapter = RecordingLoadingIndicatorAdapter()
        fragmentImpl.mActivityIndicatorBackground = View(activity).apply { visibility = View.INVISIBLE }
        fragmentImpl.mLoadingIndicator = loadingIndicatorOwning(adapter)

        // When: the invoice is retrieved for the scanned QR code
        fragmentImpl.analyzeQRCode(qrCodeDocument())

        // Then: the dim and the indicator are both up
        assertEquals(View.VISIBLE, fragmentImpl.mActivityIndicatorBackground.visibility)
        assertEquals(1, adapter.visibleCalls)
    }

    /**
     * Matches iOS (`QRCodeOverlay.showAnimation` shows the education view *or* the loading
     * indicator, never both): the education message is what the user watches, and the dim of its
     * overlay let an indicator behind it show through.
     */
    @Test
    fun `analyzeQRCode shows no loading indicator while the QR-code education is on screen`() {
        // Given: the education half of the QR-code step is running
        GiniCapture.Builder().setGiniCaptureNetworkService(mock<GiniCaptureNetworkService>()).build()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).get()
        val fragmentImpl = qrCodeFragmentImpl(activity)
        val adapter = RecordingLoadingIndicatorAdapter()
        fragmentImpl.mActivityIndicatorBackground = View(activity).apply { visibility = View.INVISIBLE }
        fragmentImpl.mLoadingIndicator = loadingIndicatorOwning(adapter)
        fragmentImpl.qrCodeEducationPopup = mockk(relaxed = true)
        fragmentImpl.mPaymentQRCodePopup = mockk(relaxed = true)
        coEvery { qrEducationTypeUseCase.execute() } returns QrEducationType.PHOTO_DOC
        every { educationFeatureEnabledUseCase.invoke() } returns true
        fragmentImpl.showQrCodePopup(paymentQRCodeData()) { }

        // When: the education half starts the invoice retrieval behind its overlay
        fragmentImpl.analyzeQRCode(qrCodeDocument())

        // Then: the dim still blocks the controls, but no indicator runs behind the message
        assertEquals(View.VISIBLE, fragmentImpl.mActivityIndicatorBackground.visibility)
        assertTrue(fragmentImpl.mActivityIndicatorBackground.isClickable)
        assertEquals(0, adapter.visibleCalls)
    }

    private fun qrCodeFragmentImpl(activity: FragmentActivity): CameraFragmentImplWithoutQRCodeReader {
        val fragmentCallback = mock<FragmentImplCallback> {
            on { this.activity } doReturn activity
        }
        return CameraFragmentImplWithoutQRCodeReader(fragmentCallback, mock<CancelListener>(), false)
    }

    private fun paymentQRCodeData() = PaymentQRCodeData(
        PaymentQRCodeData.Format.EPC069_12,
        "BCD\n002\n1\nSCT\nGENODEF1S04\nGini GmbH\nDE75512108001245126199\nEUR12.50\n\n\nInvoice 42",
        "Gini GmbH",
        "Invoice 42",
        "DE75512108001245126199",
        "GENODEF1S04",
        "12.50:EUR"
    )

    private fun qrCodeDocument(): QRCodeDocument = QRCodeDocument.fromPaymentQRCodeData(paymentQRCodeData())

    /**
     * An [InjectedViewContainer] that owns [adapter]: `modifyAdapterIfOwned` runs its lambda on it.
     * The real container only hands the adapter out once it is attached to a window, which a JVM
     * test has not got.
     */
    private fun loadingIndicatorOwning(
        adapter: CustomLoadingIndicatorAdapter
    ): InjectedViewContainer<CustomLoadingIndicatorAdapter> = mockk {
        every { injectedViewAdapterHolder } returns mockk()
        every { modifyAdapterIfOwned(any()) } answers {
            firstArg<(CustomLoadingIndicatorAdapter) -> Unit>().invoke(adapter)
        }
    }

    private class RecordingLoadingIndicatorAdapter : CustomLoadingIndicatorAdapter {
        var visibleCalls = 0

        override fun onCreateView(container: ViewGroup): View = View(container.context)
        override fun onVisible() {
            visibleCalls++
        }
        override fun onHidden() = Unit
        override fun onDestroy() = Unit
    }

    private open class CameraFragmentImplWithoutQRCodeReader(fragment: FragmentImplCallback,
                                                        cancelListener: CancelListener, addPages: Boolean
    ) : CameraFragmentImpl(fragment, cancelListener, addPages) {
            override fun initQRCodeReader() {
                // Do nothing, because no QR code reader is available in JVM tests
            }

            fun isOnlyQRCodeScanningEnabledForTest() = isOnlyQRCodeScanningEnabled()
    }
}