package net.gini.android.capture.review.multipage

import android.app.Application
import android.content.DialogInterface
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.document.GiniCaptureDocument
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.error.ErrorType
import net.gini.android.capture.internal.network.FailureException
import net.gini.android.capture.internal.network.NetworkRequestsManager
import net.gini.android.capture.internal.util.FileImportHelper
import net.gini.android.capture.review.multipage.previews.PreviewPagesAdapter
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent
import net.gini.android.capture.tracking.EventTrackingHelper.trackReviewScreenEvent
import net.gini.android.capture.tracking.ReviewScreenEvent
import net.gini.android.capture.tracking.ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Internal use only.
 *
 * ViewModel of the multi-page review screen. Owns the multi-page document, the upload state and
 * the document management business logic which used to live inline in
 * [MultiPageReviewFragment]. The fragment observes [uiState] for renderable state and [events]
 * for one-shot effects.
 */
internal open class ReviewViewModel(
    private val app: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ReviewEvent>(extraBufferCapacity = EVENT_BUFFER_SIZE)
    val events: SharedFlow<ReviewEvent> = _events.asSharedFlow()

    @VisibleForTesting
    @JvmField
    var documentUploadResults: MutableMap<String, Boolean> = HashMap()

    var multiPageDocument: ImageMultiPageDocument? = null
        @VisibleForTesting set

    private var nextClicked = false

    /**
     * (Re-)reads the multi-page document from the memory store and (re-)initializes the upload
     * results for its pages.
     *
     * @return true when the document has more pages than known before (e.g. the user added pages
     * with the camera), so the view can refresh the previews
     */
    fun initMultiPageDocument(): Boolean {
        if (GiniCapture.hasInstance()) {
            multiPageDocument = GiniCapture.getInstance().internal()
                .imageMultiPageDocumentMemoryStore.multiPageDocument
        }
        val document = multiPageDocument
            ?: throw IllegalStateException(
                "MultiPageReviewFragment requires an ImageMultiPageDocuments."
            )

        val hasNewPages = documentUploadResults.size < document.documents.size
        for (imageDocument in document.documents) {
            documentUploadResults[imageDocument.id] = false
        }
        return hasNewPages
    }

    fun onScreenResumed() {
        nextClicked = false
    }

    fun onProceedToAnalysis() {
        nextClicked = true
    }

    /**
     * Shows the "app is default for opening this file type" alert if needed and then uploads the
     * document's pages.
     */
    fun showAlertIfOpenWithAndUploadDocuments() {
        val document = multiPageDocument ?: return
        FileImportHelper.showAlertIfOpenWithDocumentAndAppIsDefault(
            app,
            document,
            { message, positiveButtonTitle, positiveButtonClickListener,
              negativeButtonTitle, negativeButtonClickListener, cancelListener ->
                emitEvent(
                    ReviewEvent.ShowAlertDialog(
                        message, positiveButtonTitle, positiveButtonClickListener,
                        negativeButtonTitle, negativeButtonClickListener, cancelListener
                    )
                )
            }
        ) { emitEvent(ReviewEvent.OpenApplicationDetailsSettings) }
            .thenRun { uploadDocuments() }
    }

    @VisibleForTesting
    fun uploadDocuments() {
        val document = multiPageDocument ?: return
        for (imageDocument in ArrayList(document.documents)) {
            if (!document.hasDocumentError(imageDocument)) {
                // Documents with an error should not be uploaded automatically
                uploadDocument(imageDocument)
            } else {
                val documentError = document.getErrorForDocument(imageDocument)
                if (documentError != null) {
                    trackAnalysisScreenEvent(AnalysisScreenEvent.ERROR)
                    val errorType =
                        ErrorType.typeFromDocumentErrorCode(documentError.errorCode)
                    emitEvent(ReviewEvent.NavigateToError(errorType, imageDocument))
                }
            }
        }
    }

    @VisibleForTesting
    open fun uploadDocument(document: ImageDocument) {
        if (!GiniCapture.hasInstance()) {
            return
        }
        val networkRequestsManager =
            GiniCapture.getInstance().internal().networkRequestsManager ?: return

        _uiState.update { it.copy(uploadIndicatorVisible = true) }

        multiPageDocument?.removeErrorForDocument(document)
        documentUploadResults[document.id] = false
        networkRequestsManager.upload(app, document)
            .handle { requestResult, throwable ->
                if (throwable != null && !NetworkRequestsManager.isCancellation(throwable)) {
                    _uiState.update { it.copy(uploadIndicatorVisible = false) }
                    trackUploadError(throwable)
                    handleUploadError(throwable, document)
                } else if (requestResult != null) {
                    documentUploadResults[document.id] = true
                }
                updateNextButtonState()
                null
            }
    }

    private fun trackUploadError(throwable: Throwable) {
        val errorDetails = mapOf(
            UPLOAD_ERROR_DETAILS_MAP_KEY.MESSAGE to throwable.message,
            UPLOAD_ERROR_DETAILS_MAP_KEY.ERROR_OBJECT to throwable
        )
        trackReviewScreenEvent(ReviewScreenEvent.UPLOAD_ERROR, errorDetails)
    }

    private fun handleUploadError(throwable: Throwable, document: Document) {
        val failureException = FailureException.tryCastFromCompletableFutureThrowable(throwable)
        trackAnalysisScreenEvent(AnalysisScreenEvent.ERROR)
        val errorType = failureException?.errorType ?: ErrorType.GENERAL
        emitEvent(ReviewEvent.NavigateToError(errorType, document))
    }

    fun onDeleteDocument(document: ImageDocument) {
        val multiPageDocument = multiPageDocument ?: return
        val wasLastPage = multiPageDocument.documents.size == 1
        val deletedPosition = multiPageDocument.documents.indexOf(document)

        deleteDocument(document)

        val newPosition = PreviewPagesAdapter.getNewPositionAfterDeletion(
            deletedPosition,
            multiPageDocument.documents.size
        )
        updateNextButtonState()
        emitEvent(ReviewEvent.PageDeleted(deletedPosition, newPosition, wasLastPage))
    }

    private fun deleteDocument(document: ImageDocument) {
        deleteFromMultiPageDocument(document)
        deleteFromCaches(document)
        deleteFromDisk(document)
        deleteFromGiniApi(document)
        documentUploadResults.remove(document.id)
    }

    private fun deleteFromMultiPageDocument(document: ImageDocument) {
        val multiPageDocument = multiPageDocument ?: return
        multiPageDocument.documents.remove(document)
        if (multiPageDocument.documents.isEmpty() && GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.clear()
        }
    }

    private fun deleteFromCaches(document: ImageDocument) {
        if (GiniCapture.hasInstance()) {
            val gcInternal = GiniCapture.getInstance().internal()
            gcInternal.documentDataMemoryCache.invalidate(document)
            gcInternal.photoMemoryCache.invalidate(document)
        }
    }

    private fun deleteFromDisk(document: ImageDocument) {
        if (GiniCapture.hasInstance()) {
            val uri = document.uri
            if (uri != null) {
                GiniCapture.getInstance().internal().imageDiskStore.delete(uri)
            }
        }
    }

    private fun deleteFromGiniApi(document: ImageDocument) {
        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().networkRequestsManager?.delete(document)
        }
    }

    /**
     * Recomputes the next button and upload indicator state from the upload results. Mirrors the
     * former fragment logic: the button is enabled and the indicator hidden only when all pages
     * uploaded successfully.
     */
    private fun updateNextButtonState() {
        val document = multiPageDocument
        if (document == null || document.documents.isEmpty()) {
            _uiState.update { it.copy(nextButtonEnabled = false) }
            return
        }
        val uploadFailed = documentUploadResults.values.any { !it }
        _uiState.update {
            it.copy(
                nextButtonEnabled = !uploadFailed,
                uploadIndicatorVisible = if (uploadFailed) it.uploadIndicatorVisible else false
            )
        }
    }

    fun onScreenPaused() {
        val multiPageDocument = multiPageDocument ?: return
        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore
                .setMultiPageDocument(multiPageDocument)
        }
    }

    /**
     * Called when the fragment is destroyed without a saved instance state, meaning it won't be
     * restarted. If the user didn't proceed to analysis, the review screen acts as the root
     * screen of the SDK and its destruction means the user exits the SDK.
     */
    fun onNonRestartingDestroy() {
        if (!nextClicked) {
            deleteUploadedDocuments()
            clearMultiPageDocument()
        }
    }

    private fun deleteUploadedDocuments() {
        val multiPageDocument = multiPageDocument ?: return
        if (GiniCapture.hasInstance()) {
            val networkRequestsManager =
                GiniCapture.getInstance().internal().networkRequestsManager
            if (networkRequestsManager != null) {
                networkRequestsManager.cancel(multiPageDocument)
                networkRequestsManager.delete(multiPageDocument)
                    .handle { _, _ ->
                        for (document in multiPageDocument.documents) {
                            val giniCaptureDocument = document as GiniCaptureDocument
                            networkRequestsManager.cancel(giniCaptureDocument)
                            networkRequestsManager.delete(giniCaptureDocument)
                        }
                        null
                    }
            }
        }
    }

    private fun clearMultiPageDocument() {
        if (GiniCapture.hasInstance()) {
            multiPageDocument = null
            GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.clear()
        }
    }

    private fun emitEvent(event: ReviewEvent) {
        if (!_events.tryEmit(event)) {
            LOG.error("Event buffer overflow, dropped event: {}", event)
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewViewModel(app) as T
        }
    }

    companion object {
        private const val EVENT_BUFFER_SIZE = 64
        private val LOG: Logger = LoggerFactory.getLogger(ReviewViewModel::class.java)

        @JvmStatic
        fun initialUiState() = ReviewUiState()
    }
}

/**
 * Internal use only.
 *
 * Renderable state of the multi-page review screen.
 */
internal data class ReviewUiState(
    val uploadIndicatorVisible: Boolean = false,
    val nextButtonEnabled: Boolean = false
)

/**
 * Internal use only.
 *
 * One-shot effects emitted by [ReviewViewModel] and handled by [MultiPageReviewFragment].
 */
internal sealed class ReviewEvent {

    data class ShowAlertDialog(
        val message: String,
        val positiveButtonTitle: String,
        val positiveButtonClickListener: DialogInterface.OnClickListener,
        val negativeButtonTitle: String?,
        val negativeButtonClickListener: DialogInterface.OnClickListener?,
        val cancelListener: DialogInterface.OnCancelListener?
    ) : ReviewEvent()

    data class NavigateToError(
        val errorType: ErrorType,
        val document: Document
    ) : ReviewEvent()

    /**
     * A page was deleted from the multi-page document. When [wasLastPage] is true the user
     * deleted the only remaining page and the view must navigate back to the camera.
     */
    data class PageDeleted(
        val deletedPosition: Int,
        val newPosition: Int,
        val wasLastPage: Boolean
    ) : ReviewEvent()

    object OpenApplicationDetailsSettings : ReviewEvent()
}
