package net.gini.android.capture.camera

import net.gini.android.capture.Document
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.document.QRCodeDocument
import net.gini.android.capture.error.ErrorType
import net.gini.android.capture.internal.qrcode.PaymentQRCodeData
import net.gini.android.capture.internal.qreducation.model.QrEducationType
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * One-shot commands emitted by [CameraViewModel] and executed by the camera fragment layer
 * ([CameraFragmentImpl]).
 *
 * They mirror the view side effects of the former fragment-as-controller implementation:
 * popups, navigation, activity indicator, thumbnail updates and [CameraFragmentListener]
 * notifications (the `Notify*`/`RequestCheckImportedDocument` events, which the fragment
 * forwards to the listener).
 *
 * Internal use only.
 */
internal sealed class CameraViewEvent {

    /** Show the loading indicator and disable user interaction. */
    object ShowActivityIndicator : CameraViewEvent()

    /** Hide the loading indicator and re-enable user interaction. */
    object HideActivityIndicator : CameraViewEvent()

    /** Hide the detected IBANs overlay. */
    object HideIbanDetected : CameraViewEvent()

    /** Hide the document corner guides. */
    object HideImageCorners : CameraViewEvent()

    /** Show the popup for unsupported (non-payment) QR codes. */
    object ShowUnsupportedQRCodePopup : CameraViewEvent()

    /** Show the payment QR code popup for the given data. */
    class ShowPaymentQRCodePopup(val data: PaymentQRCodeData) : CameraViewEvent()

    /** Show the QR code education flow. [onComplete] must be invoked when education finished. */
    class ShowQRCodeEducation(
        val educationType: QrEducationType,
        val onComplete: () -> Unit
    ) : CameraViewEvent()

    /** Hide the payment QR code popup. */
    object HidePaymentQRCodePopup : CameraViewEvent()

    /** Navigate to the no results screen for the given QR code document. */
    class NavigateToNoResults(val document: QRCodeDocument) : CameraViewEvent()

    /**
     * Navigate to the error screen via [net.gini.android.capture.error.ErrorFragment]'s
     * navigation helper (used for QR code analysis failures).
     */
    class ShowError(val errorType: ErrorType, val document: Document?) : CameraViewEvent()

    /**
     * Navigate directly to the error screen (used when the [net.gini.android.capture.GiniCapture]
     * instance is missing).
     */
    class NavigateToError(val errorType: ErrorType, val document: Document?) : CameraViewEvent()

    /** Navigate to the analysis screen for the given (non-reviewable) document. */
    class NavigateToAnalysis(
        val document: Document,
        val errorMessage: String
    ) : CameraViewEvent()

    /** Proceed to the multi-page review screen. */
    class ProceedToMultiPageReview(val shouldScrollToLastPage: Boolean) : CameraViewEvent()

    /**
     * The multi-page state changed: the thumbnail and the navigation bar buttons have to be
     * updated. When [inMultiPageState] is `false` the thumbnail must be cleared.
     */
    class MultiPageStateChanged(val inMultiPageState: Boolean) : CameraViewEvent()

    /** Reload the photo thumbnail from the current multi-page document. */
    object UpdatePhotoThumbnail : CameraViewEvent()

    /** Show the invalid file alert dialog with the given message. */
    class ShowInvalidFileAlert(val message: String) : CameraViewEvent()

    /** Show the multi-page limit reached alert dialog. */
    object ShowMultiPageLimitError : CameraViewEvent()

    /**
     * Ask the client (via [CameraFragmentListener.onCheckImportedDocument]) to check the imported
     * document. The [callback] routes the client's decision back into the [CameraViewModel].
     */
    class RequestCheckImportedDocument(
        val document: Document,
        val callback: CameraFragmentListener.DocumentCheckResultCallback
    ) : CameraViewEvent()

    /** Notify the [CameraFragmentListener] about an error. */
    class NotifyError(val error: GiniCaptureError) : CameraViewEvent()

    /** Notify the [CameraFragmentListener] that extractions are available. */
    class NotifyExtractionsAvailable(
        val extractions: Map<String, GiniCaptureSpecificExtraction>
    ) : CameraViewEvent()

    /** The camera permission is granted: open the camera. */
    object OpenCamera : CameraViewEvent()

    /** Show the no camera permission view. */
    object ShowNoPermissionView : CameraViewEvent()

    /** Hide the no camera permission view. */
    object HideNoPermissionView : CameraViewEvent()
}
