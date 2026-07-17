package net.gini.android.capture.review.multipage

import android.content.DialogInterface
import net.gini.android.capture.Document
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.error.ErrorType

/**
 * One-shot commands emitted by [MultiPageReviewViewModel] and executed by
 * [MultiPageReviewFragment].
 *
 * They mirror the view and navigation calls which the fragment executed itself before the
 * MVP to MVVM migration of the Multi-Page Review screen.
 *
 * Internal use only.
 */
internal sealed class MultiPageReviewViewEvent {

    /**
     * There are no pages to review: the hosting activity must be finished.
     */
    object FinishActivity : MultiPageReviewViewEvent()

    class ShowAlertDialog(
        val message: String,
        val positiveButtonTitle: String,
        val positiveButtonClickListener: DialogInterface.OnClickListener,
        val negativeButtonTitle: String?,
        val negativeButtonClickListener: DialogInterface.OnClickListener?,
        val cancelListener: DialogInterface.OnCancelListener?
    ) : MultiPageReviewViewEvent()

    /**
     * The page at [deletedPosition] was deleted: the fragment must update the previews, the tab
     * indicator and the scroll position.
     */
    class PageDeleted(val deletedPosition: Int) : MultiPageReviewViewEvent()

    /**
     * The last remaining page was deleted: the camera must be shown to capture the first page.
     */
    object NavigateToCameraForFirstPage : MultiPageReviewViewEvent()

    class NavigateToAnalysis(
        val document: ImageMultiPageDocument,
        val shouldSaveInvoicesLocally: Boolean
    ) : MultiPageReviewViewEvent()

    /**
     * A page has a document error: the error screen must be shown.
     */
    class NavigateToError(
        val errorType: ErrorType,
        val document: Document
    ) : MultiPageReviewViewEvent()

    /**
     * Uploading a page failed: the error screen must be shown unless it is already visible.
     */
    class NavigateToUploadError(
        val errorType: ErrorType,
        val document: Document
    ) : MultiPageReviewViewEvent()
}
