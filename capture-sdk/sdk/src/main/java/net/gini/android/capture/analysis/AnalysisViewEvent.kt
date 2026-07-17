package net.gini.android.capture.analysis

import android.content.DialogInterface
import android.graphics.Bitmap
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.analysis.education.EducationCompleteListener
import net.gini.android.capture.analysis.paymentDueHint.PaymentDueHintDismissListener
import net.gini.android.capture.analysis.warning.WarningType
import net.gini.android.capture.error.ErrorType
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * One-shot commands emitted by [AnalysisViewModel] and executed by [AnalysisFragment].
 *
 * They mirror the view methods of the former MVP `AnalysisScreenContract.View` and the
 * [AnalysisFragmentListener] callbacks (the `Notify*` events, which the fragment forwards
 * to the listener).
 *
 * Internal use only.
 */
internal sealed class AnalysisViewEvent {

    /**
     * The fragment must wait for its view layout and afterwards call
     * [AnalysisViewModel.onViewLayoutFinished].
     */
    object WaitForViewLayout : AnalysisViewEvent()

    object ShowPdfInfoPanel : AnalysisViewEvent()

    data class ShowPdfTitle(val title: String) : AnalysisViewEvent()

    class ShowBitmap(
        val bitmap: Bitmap?,
        val rotationForDisplay: Int
    ) : AnalysisViewEvent()

    class ShowAlertDialog(
        val message: String,
        val positiveButtonTitle: String,
        val positiveButtonClickListener: DialogInterface.OnClickListener,
        val negativeButtonTitle: String?,
        val negativeButtonClickListener: DialogInterface.OnClickListener?,
        val cancelListener: DialogInterface.OnCancelListener?
    ) : AnalysisViewEvent()

    class ShowHints(val hints: List<AnalysisHint>) : AnalysisViewEvent()

    class ShowErrorMessage(
        val message: String,
        val document: Document
    ) : AnalysisViewEvent()

    class ShowErrorType(
        val errorType: ErrorType,
        val document: Document
    ) : AnalysisViewEvent()

    class ShowAlreadyPaidWarning(
        val warningType: WarningType,
        val onProceed: Runnable
    ) : AnalysisViewEvent()

    class ShowPaymentDueHint(
        val dueDate: String,
        val dismissListener: PaymentDueHintDismissListener
    ) : AnalysisViewEvent()

    class ShowEducation(val listener: EducationCompleteListener) : AnalysisViewEvent()

    object ProcessInvoiceSaving : AnalysisViewEvent()

    class NotifyError(val error: GiniCaptureError) : AnalysisViewEvent()

    class NotifyExtractionsAvailable(
        val extractions: Map<String, GiniCaptureSpecificExtraction>,
        val compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
        val returnReasons: List<GiniCaptureReturnReason>
    ) : AnalysisViewEvent()

    class NotifyProceedToNoExtractionsScreen(val document: Document) : AnalysisViewEvent()

    object NotifyDefaultPDFAppAlertDialogCancelled : AnalysisViewEvent()
}
