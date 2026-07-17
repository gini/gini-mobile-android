package net.gini.android.capture.review.multipage

import android.app.Activity
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jersey.repackaged.jsr166e.CompletableFuture
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.analysis.ConsumableEvent
import net.gini.android.capture.document.GiniCaptureDocument
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.error.ErrorType
import net.gini.android.capture.internal.network.FailureException
import net.gini.android.capture.internal.network.NetworkRequestResult
import net.gini.android.capture.internal.network.NetworkRequestsManager
import net.gini.android.capture.internal.util.FileImportHelper
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent
import net.gini.android.capture.tracking.EventTrackingHelper.trackReviewScreenEvent
import net.gini.android.capture.tracking.ReviewScreenEvent
import net.gini.android.capture.tracking.ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty

/**
 * ViewModel for the Multi-Page Review screen.
 *
 * Contains the presentation logic which used to live in [MultiPageReviewFragment]: management of
 * the multi-page document, adding/deleting pages, uploading the pages and tracking the upload
 * state per page, upload error handling, navigation decisions and analytics.
 *
 * Screen state is exposed via [nextButtonEnabled] and [loadingIndicatorActive] and one-shot
 * commands via [events] which are executed by [MultiPageReviewFragment].
 *
 * Internal use only.
 */
internal open class MultiPageReviewViewModel : ViewModel() {

    @VisibleForTesting
    var documentUploadResults: MutableMap<String, Boolean> = HashMap()

    @VisibleForTesting
    var multiPageDocument: ImageMultiPageDocument? = null

    private var nextClicked: Boolean = false

    private val screenName: UserAnalyticsScreen = UserAnalyticsScreen.Review

    private val userAnalyticsEventTracker: UserAnalyticsEventTracker?
        get() = UserAnalytics.getAnalyticsEventTracker()

    private val mutableNextButtonEnabled = MutableLiveData<Boolean>()

    /**
     * Whether the next ("Process documents") button should be enabled.
     */
    val nextButtonEnabled: LiveData<Boolean> = mutableNextButtonEnabled

    private val mutableLoadingIndicatorActive = MutableLiveData<Boolean>()

    /**
     * Whether the upload loading indicator should be visible.
     */
    val loadingIndicatorActive: LiveData<Boolean> = mutableLoadingIndicatorActive

    // One-shot commands are kept in a queue and consumers are notified via the [events] signal.
    // A queue is used (instead of putting the event into the LiveData value) so that no event is
    // lost when several events are emitted in quick succession or re-entrantly while a previous
    // event is being dispatched (LiveData coalesces values, including nested setValue calls).
    private val eventQueue = ArrayDeque<MultiPageReviewViewEvent>()

    private val mutableEvents = MutableLiveData<ConsumableEvent<Unit>>()

    /**
     * Signals that one or more one-shot commands are available. Consumers must drain the pending
     * commands with [pollEvent] whenever an unhandled signal is received.
     */
    val events: LiveData<ConsumableEvent<Unit>> = mutableEvents

    /**
     * Returns the next pending one-shot command or `null` if there is none.
     */
    fun pollEvent(): MultiPageReviewViewEvent? = synchronized(eventQueue) {
        eventQueue.removeFirstOrNull()
    }

    /**
     * (Re-)loads the multi-page document from the memory store and initializes the per-page
     * upload results. Must be called from the fragment's `onCreate()` and `onResume()`.
     *
     * @return `true` if new pages were added since the last call (in which case the fragment
     * must refresh the previews), `false` otherwise
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

        if (document.documents.isEmpty()) {
            emitEvent(MultiPageReviewViewEvent.FinishActivity)
        }

        return initUploadResults(document)
    }

    private fun initUploadResults(document: ImageMultiPageDocument): Boolean {
        val hasNewPages = documentUploadResults.size < document.documents.size

        for (imageDocument in document.documents) {
            documentUploadResults[imageDocument.id] = false
        }

        return hasNewPages
    }

    /**
     * Starts the "open with" alert check and afterwards uploads the pages. Must be called from
     * the fragment's `onResume()`.
     *
     * The [activity] is only used synchronously to check whether the app is the default app for
     * "open with" documents and to start the uploads; it is not retained.
     */
    fun onResume(activity: Activity) {
        nextClicked = false
        val document = multiPageDocument ?: return
        showAlertIfOpenWithDocumentAndAppIsDefault(activity, document,
            FileImportHelper.ShowAlertCallback { message, positiveButtonTitle,
                                                 positiveButtonClickListener, negativeButtonTitle,
                                                 negativeButtonClickListener, cancelListener ->
                emitEvent(
                    MultiPageReviewViewEvent.ShowAlertDialog(
                        message, positiveButtonTitle, positiveButtonClickListener,
                        negativeButtonTitle, negativeButtonClickListener, cancelListener
                    )
                )
            })
            .thenRun { uploadDocuments(activity) }
    }

    @VisibleForTesting
    open fun showAlertIfOpenWithDocumentAndAppIsDefault(
        activity: Activity,
        document: GiniCaptureDocument,
        showAlertCallback: FileImportHelper.ShowAlertCallback
    ): CompletableFuture<Void> {
        return FileImportHelper.showAlertIfOpenWithDocumentAndAppIsDefault(
            activity, document, showAlertCallback
        )
    }

    private fun uploadDocuments(activity: Activity) {
        val document = multiPageDocument ?: return
        for (imageDocument in document.documents) {
            if (!document.hasDocumentError(imageDocument)) {
                // Documents with an error should not be uploaded automatically
                uploadDocument(imageDocument, activity)
            } else {
                val documentError = document.getErrorForDocument(imageDocument)
                if (documentError != null) {
                    trackAnalysisScreenEvent(AnalysisScreenEvent.ERROR)
                    val errorType =
                        ErrorType.typeFromDocumentErrorCode(documentError.errorCode)
                    emitEvent(
                        MultiPageReviewViewEvent.NavigateToError(errorType, imageDocument)
                    )
                }
            }
        }
    }

    /**
     * Uploads the given page. Also used to retry the upload of a page after an upload error.
     *
     * The [activity] is only used to start the upload request and is not retained.
     */
    fun uploadDocument(document: ImageDocument, activity: Activity) {
        if (!GiniCapture.hasInstance()) {
            return
        }
        val networkRequestsManager =
            GiniCapture.getInstance().internal().networkRequestsManager ?: return
        val multiPageDocument = this.multiPageDocument ?: return

        showIndicator()

        multiPageDocument.removeErrorForDocument(document)
        documentUploadResults[document.id] = false
        networkRequestsManager.upload(activity, document)
            .handle(CompletableFuture.BiFun<NetworkRequestResult<GiniCaptureDocument>,
                    Throwable, Void> { requestResult, throwable ->
                if (throwable != null
                    && !NetworkRequestsManager.isCancellation(throwable)
                ) {
                    hideIndicator()

                    trackUploadError(throwable)

                    handleError(throwable, document)
                } else if (requestResult != null) {
                    documentUploadResults[document.id] = true
                }
                updateNextButtonVisibility()
                null
            })
    }

    private fun trackUploadError(throwable: Throwable) {
        val errorDetails = mutableMapOf<String, Any?>()
        errorDetails[UPLOAD_ERROR_DETAILS_MAP_KEY.MESSAGE] = throwable.message
        errorDetails[UPLOAD_ERROR_DETAILS_MAP_KEY.ERROR_OBJECT] = throwable
        trackReviewScreenEvent(ReviewScreenEvent.UPLOAD_ERROR, errorDetails)
    }

    private fun handleError(throwable: Throwable, document: Document) {
        val failureException =
            FailureException.tryCastFromCompletableFutureThrowable(throwable)
        trackAnalysisScreenEvent(AnalysisScreenEvent.ERROR)
        val errorType = failureException?.errorType ?: ErrorType.GENERAL
        emitEvent(MultiPageReviewViewEvent.NavigateToUploadError(errorType, document))
    }

    /**
     * Deletes the given page and emits the events required to update the view. When the last
     * page was deleted the camera is requested to capture a new first page.
     */
    fun onDeleteDocument(document: ImageDocument) {
        val multiPageDocument = this.multiPageDocument ?: return
        val wasLastPage = multiPageDocument.documents.size == 1

        val deletedPosition = multiPageDocument.documents.indexOf(document)

        deleteDocument(document)

        emitEvent(MultiPageReviewViewEvent.PageDeleted(deletedPosition))

        updateNextButtonVisibility()

        if (wasLastPage) {
            emitEvent(MultiPageReviewViewEvent.NavigateToCameraForFirstPage)
        }
    }

    private fun deleteDocument(document: ImageDocument) {
        deleteFromMultiPageDocument(document)
        deleteFromCaches(document)
        deleteFromDisk(document)
        deleteFromGiniApi(document)
        documentUploadResults.remove(document.id)
    }

    private fun deleteFromMultiPageDocument(document: ImageDocument) {
        val multiPageDocument = this.multiPageDocument ?: return
        multiPageDocument.documents.remove(document)
        if (multiPageDocument.documents.size == 0 && GiniCapture.hasInstance()) {
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
            val networkRequestsManager =
                GiniCapture.getInstance().internal().networkRequestsManager
            networkRequestsManager?.delete(document)
        }
    }

    /**
     * Recomputes whether the next button should be enabled from the per-page upload results and
     * hides the loading indicator when all pages were uploaded successfully.
     */
    fun updateNextButtonVisibility() {
        val document = multiPageDocument
        if (document == null || document.documents.isEmpty()) {
            setNextButtonEnabled(false)
            return
        }

        var uploadFailed = false
        for (uploadSuccess in documentUploadResults.values) {
            if (!uploadSuccess) {
                uploadFailed = true
                break
            }
        }
        if (!uploadFailed) {
            hideIndicator()
        }
        setNextButtonEnabled(!uploadFailed)
    }

    /**
     * Must be called when the user proceeds to the Analysis screen.
     *
     * @param shouldSaveInvoicesLocally whether the invoices should be saved locally (evaluated
     * by the fragment from the "save invoices" view state)
     */
    fun onNextButtonClicked(shouldSaveInvoicesLocally: Boolean) {
        trackReviewScreenEvent(ReviewScreenEvent.NEXT)
        val document = multiPageDocument ?: return
        userAnalyticsEventTracker?.trackEvent(
            UserAnalyticsEvent.PROCEED_TAPPED,
            setOf(
                UserAnalyticsEventProperty.Screen(screenName),
                UserAnalyticsEventProperty.DocumentPageNumber(document.documents.size)
            )
        )
        nextClicked = true
        emitEvent(
            MultiPageReviewViewEvent.NavigateToAnalysis(document, shouldSaveInvoicesLocally)
        )
    }

    /**
     * Must be called when the user navigates back (via the back button or the top navigation
     * bar close button).
     */
    fun onBackClicked() {
        trackReviewScreenEvent(ReviewScreenEvent.BACK)
        trackUserAnalyticsEvent(UserAnalyticsEvent.CLOSE_TAPPED)
    }

    fun trackScreenShownEvent() {
        trackUserAnalyticsEvent(UserAnalyticsEvent.SCREEN_SHOWN)
    }

    fun onAddPagesTapped() {
        trackUserAnalyticsEvent(UserAnalyticsEvent.ADD_PAGES_TAPPED)
    }

    fun onPageClicked() {
        trackUserAnalyticsEvent(UserAnalyticsEvent.FULL_SCREEN_PAGE_TAPPED)
    }

    fun onPageSwiped() {
        trackUserAnalyticsEvent(UserAnalyticsEvent.PAGE_SWIPED)
    }

    private fun trackUserAnalyticsEvent(event: UserAnalyticsEvent) {
        userAnalyticsEventTracker?.trackEvent(
            event,
            setOf(UserAnalyticsEventProperty.Screen(screenName))
        )
    }

    /**
     * Must be called from the fragment's `onPause()`.
     */
    fun onPause() {
        val document = multiPageDocument ?: return
        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore
                .setMultiPageDocument(document)
        }
    }

    /**
     * Must be called from the fragment's `onDestroy()`.
     *
     * @param instanceStateSaved whether the fragment's instance state was saved, meaning that
     * the fragment will be restarted and the documents must be kept
     */
    fun onDestroy(instanceStateSaved: Boolean) {
        if (!instanceStateSaved) {
            // Instance state wasn't saved meaning that this fragment won't restart
            if (!nextClicked) {
                // Delete documents because the Multi-Page Review Fragment
                // acts as the root screen and when it's destroyed it means
                // the user will exit the SDK
                deleteUploadedDocuments()
                clearMultiPageDocument()
            }
        }
    }

    private fun deleteUploadedDocuments() {
        val multiPageDocument = this.multiPageDocument ?: return

        if (GiniCapture.hasInstance()) {
            val networkRequestsManager =
                GiniCapture.getInstance().internal().networkRequestsManager
            if (networkRequestsManager != null) {
                networkRequestsManager.cancel(multiPageDocument)
                networkRequestsManager.delete(multiPageDocument)
                    .handle(CompletableFuture.BiFun<NetworkRequestResult<GiniCaptureDocument>,
                            Throwable, Void> { _, _ ->
                        for (document in multiPageDocument.documents) {
                            networkRequestsManager.cancel(document)
                            networkRequestsManager.delete(document)
                        }
                        null
                    })
            }
        }
    }

    private fun clearMultiPageDocument() {
        if (GiniCapture.hasInstance()) {
            multiPageDocument = null
            GiniCapture.getInstance().internal()
                .imageMultiPageDocumentMemoryStore.clear()
        }
    }

    private fun showIndicator() {
        setLiveDataValue(mutableLoadingIndicatorActive, true)
    }

    private fun hideIndicator() {
        setLiveDataValue(mutableLoadingIndicatorActive, false)
    }

    private fun setNextButtonEnabled(enabled: Boolean) {
        setLiveDataValue(mutableNextButtonEnabled, enabled)
    }

    private fun <T> setLiveDataValue(liveData: MutableLiveData<T>, value: T) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            liveData.value = value
        } else {
            liveData.postValue(value)
        }
    }

    private fun emitEvent(event: MultiPageReviewViewEvent) {
        synchronized(eventQueue) {
            eventQueue.addLast(event)
        }
        // Only a signal is set as the LiveData value: coalescing is harmless because consumers
        // drain the whole queue for a single signal.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mutableEvents.value = ConsumableEvent(Unit)
        } else {
            mutableEvents.postValue(ConsumableEvent(Unit))
        }
    }
}
