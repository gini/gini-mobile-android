package net.gini.android.capture.camera

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.analysis.ConsumableEvent
import net.gini.android.capture.document.GiniCaptureDocument
import net.gini.android.capture.document.ImageDocumentFake
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.error.ErrorType
import net.gini.android.capture.internal.qrcode.EPSPaymentParser
import net.gini.android.capture.internal.qrcode.PaymentQRCodeData
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Logic-level tests for [CameraViewModel].
 *
 * Ported from the former `CameraFragmentImplTest` cases which covered non-view-bound behavior
 * after the MVP to MVVM migration of the Camera screen. View side effects are asserted through
 * the [CameraViewModel.events] queue instead of view or listener mocks.
 */
@RunWith(AndroidJUnit4::class)
class CameraViewModelTest {

    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    private class EventRecorder : Observer<ConsumableEvent<Unit>> {

        lateinit var viewModel: CameraViewModel

        val events = mutableListOf<CameraViewEvent>()

        override fun onChanged(value: ConsumableEvent<Unit>) {
            if (value.getContentIfNotHandled() == null) {
                return
            }
            while (true) {
                val event = viewModel.pollEvent() ?: return
                events.add(event)
            }
        }

        inline fun <reified T : CameraViewEvent> firstOrNull(): T? =
            events.filterIsInstance<T>().firstOrNull()
    }

    private fun observe(viewModel: CameraViewModel): EventRecorder {
        val recorder = EventRecorder()
        recorder.viewModel = viewModel
        viewModel.events.observeForever(recorder)
        return recorder
    }

    /**
     * Waits for an event matching [predicate] while processing tasks posted to the main looper
     * (needed because listener notifications are dispatched through a background coroutine).
     */
    private fun awaitEvent(
        recorder: EventRecorder,
        timeoutMs: Long = 3000,
        predicate: (CameraViewEvent) -> Boolean
    ): CameraViewEvent? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            recorder.events.firstOrNull(predicate)?.let { return it }
            Thread.sleep(10)
        }
        return null
    }

    @Test
    fun `resets QR code state when unsupported popup hides so scanning resumes`() {
        // Given: state is set as if an unsupported QR code was shown and blocked further scans
        val viewModel = CameraViewModel(app)
        viewModel.qrCodeContent = "unsupported-qr-content"
        viewModel.interfaceHidden = true

        // When: the unsupported QR popup hides
        viewModel.onUnsupportedQRCodePopupHidden()

        // Then: state is reset so subsequent QR codes can be detected
        assertNull(viewModel.qrCodeContent)
        assertFalse(viewModel.interfaceHidden)
    }

    @Test
    fun `ignores detected QR codes while the interface is hidden`() {
        // Given
        val viewModel = CameraViewModel(app)
        viewModel.interfaceHidden = true
        val recorder = observe(viewModel)

        // When
        viewModel.onQRCodeDetected(null, "qr-content", false, false)

        // Then
        assertThat(recorder.events).isEmpty()
        assertNull(viewModel.qrCodeContent)
    }

    @Test
    fun `ignores detected QR codes while a QR code popup is shown`() {
        // Given
        val viewModel = CameraViewModel(app)
        val recorder = observe(viewModel)

        // When
        viewModel.onQRCodeDetected(null, "qr-content", true, false)

        // Then
        assertThat(recorder.events).isEmpty()
        assertFalse(viewModel.interfaceHidden)
    }

    @Test
    fun `shows unsupported QR code popup for a newly detected non-payment QR code`() {
        // Given
        val viewModel = CameraViewModel(app)
        val recorder = observe(viewModel)

        // When
        viewModel.onQRCodeDetected(null, "qr-content", false, false)

        // Then: the IBAN overlay is hidden and the unsupported QR code popup is requested
        assertThat(recorder.firstOrNull<CameraViewEvent.HideIbanDetected>()).isNotNull()
        assertThat(recorder.firstOrNull<CameraViewEvent.ShowUnsupportedQRCodePopup>()).isNotNull()
        assertThat(viewModel.qrCodeContent).isEqualTo("qr-content")
        assertTrue(viewModel.interfaceHidden)
    }

    @Test
    fun `shows unsupported QR code popup with a delay when the same QR code is detected again`() {
        // Given: the same QR code was detected before
        val viewModel = CameraViewModel(app)
        viewModel.qrCodeContent = "qr-content"
        val recorder = observe(viewModel)

        // When
        viewModel.onQRCodeDetected(null, "qr-content", false, false)

        // Then: the popup is not requested right away
        assertThat(recorder.firstOrNull<CameraViewEvent.ShowUnsupportedQRCodePopup>()).isNull()
        assertTrue(viewModel.interfaceHidden)

        // When: the delay elapsed
        shadowOf(Looper.getMainLooper())
            .idleFor(Duration.ofMillis(CameraViewModel.SAME_QRCODE_DETECTED_POPUP_DELAY_MS))

        // Then
        assertThat(recorder.firstOrNull<CameraViewEvent.ShowUnsupportedQRCodePopup>()).isNotNull()
    }

    @Test
    fun `sends the QR code scanned analytics event only once`() {
        // Given
        val viewModel = CameraViewModel(app)
        val analyticsTracker = mock<UserAnalyticsEventTracker> {
            on { trackEvent(any()) }.thenReturn(true)
            on { trackEvent(any(), any()) }.thenReturn(true)
        }
        viewModel.userAnalyticsEventTracker = analyticsTracker

        // When: the unsupported QR code popup was shown twice
        viewModel.onUnsupportedQRCodePopupShown()
        viewModel.onUnsupportedQRCodePopupShown()

        // Then: the analytics event was only sent once
        verify(analyticsTracker, times(1))
            .trackEvent(eq(UserAnalyticsEvent.QR_CODE_SCANNED), any())
    }

    @Test
    fun `notifies listener about extractions for an EPS payment QR code`() {
        // Given
        val viewModel = CameraViewModel(app)
        val analyticsTracker = mock<UserAnalyticsEventTracker> {
            on { trackEvent(any()) }.thenReturn(true)
            on { trackEvent(any(), any()) }.thenReturn(true)
        }
        viewModel.userAnalyticsEventTracker = analyticsTracker
        val recorder = observe(viewModel)

        // When
        viewModel.handlePaymentQRCodeData(
            PaymentQRCodeData(
                PaymentQRCodeData.Format.EPS_PAYMENT,
                "epc-content", null, null, null, null, null
            )
        )

        // Then: the image corner guides are hidden and the extractions are dispatched
        assertThat(recorder.firstOrNull<CameraViewEvent.HideImageCorners>()).isNotNull()
        val notifyEvent = awaitEvent(recorder) {
            it is CameraViewEvent.NotifyExtractionsAvailable
        } as? CameraViewEvent.NotifyExtractionsAvailable
        assertThat(notifyEvent).isNotNull()
        assertThat(notifyEvent!!.extractions)
            .containsKey(EPSPaymentParser.EXTRACTION_ENTITY_NAME)
        assertThat(
            notifyEvent.extractions[EPSPaymentParser.EXTRACTION_ENTITY_NAME]!!.value
        ).isEqualTo("epc-content")
        verify(analyticsTracker, times(1))
            .trackEvent(eq(UserAnalyticsEvent.QR_CODE_SCANNED), any())
    }

    @Test
    fun `initializes the multi-page state from the memory store`() {
        // Given
        GiniCapture.Builder().build()
        val multiPageDocument = ImageMultiPageDocument(
            Document.Source.newCameraSource(), Document.ImportMethod.NONE
        )
        multiPageDocument.addDocument(ImageDocumentFake())
        GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore
            .setMultiPageDocument(multiPageDocument)
        val viewModel = CameraViewModel(app)
        val recorder = observe(viewModel)

        // When
        viewModel.initMultiPageDocument()

        // Then
        assertThat(recorder.firstOrNull<CameraViewEvent.UpdatePhotoThumbnail>()).isNotNull()
        val stateChanged = recorder.firstOrNull<CameraViewEvent.MultiPageStateChanged>()
        assertThat(stateChanged).isNotNull()
        assertThat(stateChanged!!.inMultiPageState).isTrue()
        assertTrue(viewModel.isInMultiPageState())
        assertThat(viewModel.getMultiPageDocument()).isEqualTo(multiPageDocument)
    }

    @Test
    fun `clears the multi-page state when the memory store is empty`() {
        // Given
        GiniCapture.Builder().build()
        val viewModel = CameraViewModel(app)
        viewModel.restoreInMultiPageState(true)
        val recorder = observe(viewModel)

        // When
        viewModel.initMultiPageDocument()

        // Then
        val stateChanged = recorder.firstOrNull<CameraViewEvent.MultiPageStateChanged>()
        assertThat(stateChanged).isNotNull()
        assertThat(stateChanged!!.inMultiPageState).isFalse()
        assertFalse(viewModel.isInMultiPageState())
        assertNull(viewModel.getMultiPageDocument())
    }

    @Test
    fun `adds the captured image to the existing multi-page document`() {
        // Given
        GiniCapture.Builder().build()
        val multiPageDocument = ImageMultiPageDocument(
            Document.Source.newCameraSource(), Document.ImportMethod.NONE
        )
        multiPageDocument.addDocument(ImageDocumentFake())
        GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore
            .setMultiPageDocument(multiPageDocument)
        val viewModel = CameraViewModel(app)
        viewModel.initMultiPageDocument()

        // When
        val outcome = viewModel.onImageCaptured(ImageDocumentFake())

        // Then
        assertThat(outcome).isEqualTo(CameraViewModel.CapturedImageOutcome.MULTI_PAGE_ADDED)
        assertThat(multiPageDocument.documents).hasSize(2)
    }

    @Test
    fun `creates a multi-page document for the captured image when multi-page is enabled`() {
        // Given
        GiniCapture.Builder().setMultiPageEnabled(true).build()
        val viewModel = CameraViewModel(app)

        // When
        val outcome = viewModel.onImageCaptured(ImageDocumentFake())

        // Then
        assertThat(outcome).isEqualTo(CameraViewModel.CapturedImageOutcome.MULTI_PAGE_CREATED)
        assertTrue(viewModel.isInMultiPageState())
        val storedDocument = GiniCapture.getInstance().internal()
            .imageMultiPageDocumentMemoryStore.multiPageDocument
        assertThat(storedDocument).isEqualTo(viewModel.getMultiPageDocument())
        assertThat(storedDocument!!.documents).hasSize(1)
    }

    @Test
    fun `creates a single page multi-page document for the captured image when multi-page is disabled`() {
        // Given
        GiniCapture.Builder().setMultiPageEnabled(false).build()
        val viewModel = CameraViewModel(app)

        // When
        val outcome = viewModel.onImageCaptured(ImageDocumentFake())

        // Then
        assertThat(outcome).isEqualTo(CameraViewModel.CapturedImageOutcome.SINGLE_PAGE)
        assertFalse(viewModel.isInMultiPageState())
        assertNull(viewModel.getMultiPageDocument())
        val storedDocument = GiniCapture.getInstance().internal()
            .imageMultiPageDocumentMemoryStore.multiPageDocument
        assertThat(storedDocument!!.documents).hasSize(1)
    }

    @Test
    fun `routes to opening the camera when the camera permission is granted`() {
        // Given
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        val viewModel = CameraViewModel(app)
        val recorder = observe(viewModel)

        // When
        viewModel.checkCameraPermission()

        // Then
        assertThat(recorder.firstOrNull<CameraViewEvent.OpenCamera>()).isNotNull()
        assertThat(recorder.firstOrNull<CameraViewEvent.ShowNoPermissionView>()).isNull()
    }

    @Test
    fun `routes to the no permission view when the camera permission is missing`() {
        // Given
        val viewModel = CameraViewModel(app)
        val recorder = observe(viewModel)

        // When
        viewModel.checkCameraPermission()

        // Then
        assertThat(recorder.firstOrNull<CameraViewEvent.ShowNoPermissionView>()).isNotNull()
        assertThat(recorder.firstOrNull<CameraViewEvent.OpenCamera>()).isNull()
    }

    @Test
    fun `navigates to the error screen on start when the GiniCapture instance is missing`() {
        // Given
        GiniCaptureHelper.setGiniCaptureInstance(null)
        val viewModel = CameraViewModel(app)
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        val navigateToError = recorder.firstOrNull<CameraViewEvent.NavigateToError>()
        assertThat(navigateToError).isNotNull()
        assertThat(navigateToError!!.errorType).isEqualTo(ErrorType.GENERAL)
    }

    @Test
    fun `navigates to analysis when the client accepts a non-reviewable document`() {
        // Given
        GiniCapture.Builder().build()
        val document = mock<GiniCaptureDocument> {
            on { type } doReturn Document.Type.PDF
            on { isReviewable } doReturn false
        }
        val viewModel = CameraViewModel(app)
        val recorder = observe(viewModel)

        // When
        viewModel.requestClientDocumentCheck(document)
        val requestCheck = recorder.firstOrNull<CameraViewEvent.RequestCheckImportedDocument>()
        assertThat(requestCheck).isNotNull()
        assertThat(recorder.firstOrNull<CameraViewEvent.ShowActivityIndicator>()).isNotNull()
        requestCheck!!.callback.documentAccepted()

        // Then
        assertThat(recorder.firstOrNull<CameraViewEvent.HideActivityIndicator>()).isNotNull()
        val navigateToAnalysis = recorder.firstOrNull<CameraViewEvent.NavigateToAnalysis>()
        assertThat(navigateToAnalysis).isNotNull()
        assertThat(navigateToAnalysis!!.document).isEqualTo(document)
    }

    @Test
    fun `shows the invalid file alert when the client rejects a document`() {
        // Given
        GiniCapture.Builder().build()
        val document = mock<GiniCaptureDocument>()
        val viewModel = CameraViewModel(app)
        val recorder = observe(viewModel)

        // When
        viewModel.requestClientDocumentCheck(document)
        val requestCheck = recorder.firstOrNull<CameraViewEvent.RequestCheckImportedDocument>()
        assertThat(requestCheck).isNotNull()
        requestCheck!!.callback.documentRejected("Rejected by client")

        // Then
        assertThat(recorder.firstOrNull<CameraViewEvent.HideActivityIndicator>()).isNotNull()
        val invalidFileAlert = recorder.firstOrNull<CameraViewEvent.ShowInvalidFileAlert>()
        assertThat(invalidFileAlert).isNotNull()
        assertThat(invalidFileAlert!!.message).isEqualTo("Rejected by client")
        assertTrue(viewModel.isGenericErrorShowing())
        assertThat(viewModel.getCurrentGenericErrorMessage()).isEqualTo("Rejected by client")
        assertThat(viewModel.getGenericErrorType())
            .isEqualTo(CameraViewModel.ERROR_TYPE_INVALID_FILE)
    }

    @Test
    fun `re-emits the multi-page limit dialog when restoring saved dialog state`() {
        // Given
        val viewModel = CameraViewModel(app)
        val recorder = observe(viewModel)

        // When
        viewModel.restoreGenericErrorState(
            true, "", CameraViewModel.ERROR_TYPE_MULTI_PAGE
        )

        // Then
        assertThat(recorder.firstOrNull<CameraViewEvent.ShowMultiPageLimitError>()).isNotNull()
        assertTrue(viewModel.isGenericErrorShowing())
    }

    @Test
    fun `re-emits the invalid file dialog when restoring saved dialog state`() {
        // Given
        val viewModel = CameraViewModel(app)
        val recorder = observe(viewModel)

        // When
        viewModel.restoreGenericErrorState(
            true, "Invalid file", CameraViewModel.ERROR_TYPE_INVALID_FILE
        )

        // Then
        val invalidFileAlert = recorder.firstOrNull<CameraViewEvent.ShowInvalidFileAlert>()
        assertThat(invalidFileAlert).isNotNull()
        assertThat(invalidFileAlert!!.message).isEqualTo("Invalid file")
    }

    @Test
    fun `resetting the dialog state clears the generic error values`() {
        // Given
        val viewModel = CameraViewModel(app)
        viewModel.restoreGenericErrorState(
            true, "Invalid file", CameraViewModel.ERROR_TYPE_INVALID_FILE
        )

        // When
        viewModel.resetGenericDialogState()

        // Then
        assertFalse(viewModel.isGenericErrorShowing())
        assertThat(viewModel.getCurrentGenericErrorMessage()).isEmpty()
        assertThat(viewModel.getGenericErrorType()).isEmpty()
    }

    @Test
    fun `notifies the listener with the combined error message`() {
        // Given
        val viewModel = CameraViewModel(app)
        val recorder = observe(viewModel)

        // When
        viewModel.handleError(
            GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED,
            "Failed to take picture",
            RuntimeException("shutter failure")
        )

        // Then
        val notifyError = recorder.firstOrNull<CameraViewEvent.NotifyError>()
        assertThat(notifyError).isNotNull()
        assertThat(notifyError!!.error.errorCode)
            .isEqualTo(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED)
        assertThat(notifyError.error.message)
            .isEqualTo("Failed to take picture: shutter failure")
    }
}
