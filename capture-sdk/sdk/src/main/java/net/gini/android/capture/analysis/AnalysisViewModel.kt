package net.gini.android.capture.analysis

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jersey.repackaged.jsr166e.CompletableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.gini.android.capture.AsyncCallback
import net.gini.android.capture.BankSDKBridge
import net.gini.android.capture.BankSDKProperties
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.ProductTag
import net.gini.android.capture.analysis.AnalysisInteractor.ResultHolder
import net.gini.android.capture.analysis.education.EducationCompleteListener
import net.gini.android.capture.analysis.paymentDueHint.PaymentDueHintDismissListener
import net.gini.android.capture.analysis.transactiondoc.AttachedToTransactionDocumentProvider
import net.gini.android.capture.analysis.warning.WarningPaymentState
import net.gini.android.capture.analysis.warning.WarningType
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.document.DocumentFactory
import net.gini.android.capture.document.GiniCaptureDocument
import net.gini.android.capture.document.GiniCaptureDocumentError
import net.gini.android.capture.document.GiniCaptureMultiPageDocument
import net.gini.android.capture.document.PdfDocument
import net.gini.android.capture.error.ErrorType
import net.gini.android.capture.internal.camera.photo.ParcelableMemoryCache
import net.gini.android.capture.internal.document.DocumentRenderer
import net.gini.android.capture.internal.document.DocumentRendererFactory
import net.gini.android.capture.internal.network.FailureException
import net.gini.android.capture.internal.qreducation.GetInvoiceEducationTypeUseCase
import net.gini.android.capture.internal.qreducation.IncrementInvoiceRecognizedCounterUseCase
import net.gini.android.capture.internal.qreducation.model.InvoiceEducationType
import net.gini.android.capture.internal.storage.ImageDiskStore
import net.gini.android.capture.internal.util.FileImportHelper
import net.gini.android.capture.internal.util.NullabilityHelper.getListOrEmpty
import net.gini.android.capture.internal.util.NullabilityHelper.getMapOrEmpty
import net.gini.android.capture.internal.util.Size
import net.gini.android.capture.logging.ErrorLog
import net.gini.android.capture.logging.ErrorLogger
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.paymentHints.GetAlreadyPaidHintEnabledUseCase
import net.gini.android.capture.paymentHints.GetPaymentDueHintEnabledUseCase
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY
import net.gini.android.capture.tracking.EventTrackingHelper
import net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Collections
import java.util.Random

/**
 * ViewModel for the Analysis screen.
 *
 * Contains the logic of the former MVP `AnalysisScreenPresenter` and
 * `AnalysisScreenPresenterExtension`: document analysis orchestration via [AnalysisInteractor],
 * hints rotation, error handling, extraction results, education flow and cancellation.
 *
 * Screen state is exposed via [scanAnimationActive] and one-shot commands via [events] which are
 * executed by [AnalysisFragment].
 *
 * Internal use only.
 */
internal open class AnalysisViewModel(
    private val app: Application,
    document: Document,
    private val documentAnalysisErrorMessage: String?,
    private val analysisInteractor: AnalysisInteractor,
    private val isInvoiceSavingEnabled: Boolean
) : ViewModel() {

    val multiPageDocument: GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError> =
        asMultiPageDocument(document)

    val hints: List<AnalysisHint>

    @VisibleForTesting
    @JvmField
    var documentRenderer: DocumentRenderer? = null

    private var mStopped: Boolean = false
    private var mAnalysisCompleted: Boolean = false
    private var isSavingInvoicesInProgress: Boolean = false
    private var successResultHolder: ResultHolder? = null

    private var bankSDKBridge: BankSDKBridge? = null

    private val alreadyPaidHintEnabledUseCase:
            GetAlreadyPaidHintEnabledUseCase by getGiniCaptureKoin().inject()

    private val paymentDueHintEnabledUseCase:
            GetPaymentDueHintEnabledUseCase by getGiniCaptureKoin().inject()

    private val lastAnalyzedDocumentProvider: LastAnalyzedDocumentProvider
            by getGiniCaptureKoin().inject()

    private val attachDocToTransactionDialogProvider: AttachedToTransactionDocumentProvider
            by getGiniCaptureKoin().inject()

    private val getInvoiceEducationTypeUseCase: GetInvoiceEducationTypeUseCase
            by getGiniCaptureKoin().inject()

    private val incrementInvoiceRecognizedCounterUseCase: IncrementInvoiceRecognizedCounterUseCase
            by getGiniCaptureKoin().inject()

    // Owns all coroutines started for post-analysis navigation. Parented to the viewModelScope
    // job so that clearing the ViewModel also cancels it, but kept as a named child job so that
    // stop() can cancel it explicitly (see AnalysisViewModelCancellationTest).
    private val job = SupervisorJob(viewModelScope.coroutineContext[Job])
    private val scope = CoroutineScope(viewModelScope.coroutineContext + Dispatchers.IO + job)

    private val educationMutex = Mutex()

    private var invoiceEducationType: InvoiceEducationType? = null

    private val mutableScanAnimationActive = MutableLiveData(false)

    /**
     * Whether the scan animation (loading indicator and analysis message) should be visible.
     */
    val scanAnimationActive: LiveData<Boolean> = mutableScanAnimationActive

    // One-shot commands are kept in a queue and consumers are notified via the [events] signal.
    // A queue is used (instead of putting the event into the LiveData value) so that no event is
    // lost when several events are emitted in quick succession or re-entrantly while a previous
    // event is being dispatched (LiveData coalesces values, including nested setValue calls).
    private val eventQueue = ArrayDeque<AnalysisViewEvent>()

    private val mutableEvents = MutableLiveData<ConsumableEvent<Unit>>()

    /**
     * Signals that one or more one-shot commands are available. Consumers must drain the pending
     * commands with [pollEvent] whenever an unhandled signal is received.
     */
    val events: LiveData<ConsumableEvent<Unit>> = mutableEvents

    /**
     * Returns the next pending one-shot command or `null` if there is none.
     */
    fun pollEvent(): AnalysisViewEvent? = synchronized(eventQueue) {
        eventQueue.removeFirstOrNull()
    }

    init {
        // Tag the documents to be able to clean up the automatically parcelled data
        tagDocumentsForParcelableMemoryCache(document, multiPageDocument)
        hints = generateRandomHintsList()
    }

    private fun generateRandomHintsList(): List<AnalysisHint> {
        val list = AnalysisHint.getArray()
        Collections.shuffle(list, Random())
        when (multiPageDocument.type) {
            Document.Type.IMAGE_MULTI_PAGE,
            Document.Type.PDF_MULTI_PAGE,
            Document.Type.QR_CODE_MULTI_PAGE -> Unit
            else -> list.remove(AnalysisHint.MULTIPAGE)
        }
        return list
    }

    private fun tagDocumentsForParcelableMemoryCache(
        document: Document,
        multiPageDocument: GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
    ) {
        if (document is GiniCaptureDocument) {
            document.setParcelableMemoryCacheTag(PARCELABLE_MEMORY_CACHE_TAG)
        }
        multiPageDocument.setParcelableMemoryCacheTag(PARCELABLE_MEMORY_CACHE_TAG)
    }

    @Suppress("UNCHECKED_CAST")
    private fun asMultiPageDocument(
        document: Document
    ): GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError> {
        return if (document !is GiniCaptureMultiPageDocument<*, *>) {
            DocumentFactory.newMultiPageDocument(document as GiniCaptureDocument)
                    as GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
        } else {
            document as GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
        }
    }

    fun setBankSDKBridge(bankSDKBridge: BankSDKBridge?) {
        this.bankSDKBridge = bankSDKBridge
    }

    /**
     * Starts the screen logic. Must be called from the fragment's `onResume()`
     * (the former presenter was started there as well).
     */
    fun onStart() {
        mStopped = false
        checkGiniCaptureInstance()
        if (multiPageDocument.type != Document.Type.XML &&
            multiPageDocument.type != Document.Type.XML_MULTI_PAGE
        ) {
            createDocumentRenderer()
        }
        clearParcelableMemoryCache()
        setScanAnimationActive(true)
        loadDocumentData()
        showHintsForImage()
    }

    /**
     * Stops the screen logic and cancels all coroutines managing post-analysis navigation.
     * Must be called from the fragment's `onDestroy()` when the activity is not changing
     * configurations (matching the former presenter's `stop()` invocation).
     */
    fun onStop() {
        mStopped = true
        job.cancel()
        setScanAnimationActive(false)
        if (!mAnalysisCompleted) {
            deleteUploadedDocuments()
        } else if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore
                .clear()
        }
    }

    /**
     * Must be called from the fragment's `onDestroy()` when the activity is finishing.
     */
    fun finish() {
        clearParcelableMemoryCache()
    }

    @VisibleForTesting
    open fun clearParcelableMemoryCache() {
        // Remove data from the memory cache. The data had been added when the document in the
        // arguments was automatically parcelled when the activity was stopped
        ParcelableMemoryCache.getInstance().removeEntriesWithTag(PARCELABLE_MEMORY_CACHE_TAG)
    }

    private fun checkGiniCaptureInstance() {
        if (!GiniCapture.hasInstance()) {
            emitEvent(AnalysisViewEvent.ShowErrorType(ErrorType.GENERAL, multiPageDocument))
        }
    }

    private fun deleteUploadedDocuments() {
        if (multiPageDocument.type == Document.Type.PDF_MULTI_PAGE) {
            // Delete PDF partial documents here because the Camera Screen
            // doesn't keep references to them
            analysisInteractor.deleteMultiPageDocument(multiPageDocument)
        } else {
            // Delete only the composite document
            analysisInteractor.deleteDocument(multiPageDocument)
        }
    }

    @VisibleForTesting
    open fun isStopped(): Boolean = mStopped

    @VisibleForTesting
    open fun createDocumentRenderer() {
        val documentToRender = getFirstDocument()
        if (documentToRender != null) {
            documentRenderer = DocumentRendererFactory.fromDocument(documentToRender)
        }
    }

    @VisibleForTesting
    open fun getPdfFilename(pdfDocument: PdfDocument): String? = pdfDocument.filename

    @VisibleForTesting
    open fun clearSavedImages() {
        ImageDiskStore.clear(app)
    }

    private fun loadDocumentData() {
        LOG.debug("Loading document data")
        multiPageDocument.loadData(app, object : AsyncCallback<ByteArray, Exception> {
            override fun onSuccess(result: ByteArray?) {
                LOG.debug("Document data loaded")
                if (isStopped()) {
                    return
                }
                emitEvent(AnalysisViewEvent.WaitForViewLayout)
            }

            override fun onError(exception: Exception) {
                LOG.error("Failed to load document data", exception)
                if (isStopped()) {
                    return
                }
                ErrorLogger.log(
                    ErrorLog(description = "Failed to load document data", exception = exception)
                )
                emitEvent(
                    AnalysisViewEvent.NotifyError(
                        GiniCaptureError(
                            GiniCaptureError.ErrorCode.ANALYSIS,
                            "An error occurred while loading the document."
                        )
                    )
                )
            }

            override fun onCancelled() {
                // Not used
            }
        })
    }

    /**
     * Must be called by the fragment after the view layout finished in response to the
     * [AnalysisViewEvent.WaitForViewLayout] event.
     *
     * The [activity] is only used synchronously to check whether the app is the default app for
     * "open with" documents and is not retained.
     */
    fun onViewLayoutFinished(pdfPreviewSize: Size, activity: Activity) {
        LOG.debug("View layout finished")
        showPdfInfoForPdfDocument()
        showDocument(pdfPreviewSize)
        analyzeDocument(activity)
    }

    private fun showHintsForImage() {
        val invoiceEducationType = getInvoiceEducationType()
        if (getFirstDocument().type == Document.Type.IMAGE && invoiceEducationType == null) {
            emitEvent(AnalysisViewEvent.ShowHints(hints))
        }
    }

    private fun getFirstDocument(): GiniCaptureDocument = multiPageDocument.documents[0]

    private fun showPdfInfoForPdfDocument() {
        val documentToRender = getFirstDocument()
        if (documentToRender is PdfDocument) {
            emitEvent(AnalysisViewEvent.ShowPdfInfoPanel)
            val filename = getPdfFilename(documentToRender)
            if (filename != null) {
                emitEvent(AnalysisViewEvent.ShowPdfTitle(filename))
            }
        }
    }

    private fun showDocument(pdfPreviewSize: Size) {
        LOG.debug("Rendering the document")
        val renderer = documentRenderer ?: return
        renderer.toBitmap(app, pdfPreviewSize) { bitmap, rotationForDisplay ->
            LOG.debug("Document rendered")
            if (isStopped()) {
                return@toBitmap
            }

            if (multiPageDocument.type == Document.Type.IMAGE_MULTI_PAGE ||
                multiPageDocument.type == Document.Type.IMAGE
            ) {
                return@toBitmap
            }

            emitEvent(AnalysisViewEvent.ShowBitmap(bitmap, rotationForDisplay))
        }
    }

    @VisibleForTesting
    open fun analyzeDocument(activity: Activity) {
        showAlertIfOpenWithDocumentAndAppIsDefault(
            activity, multiPageDocument,
            FileImportHelper.ShowAlertCallback { message, positiveButtonTitle,
                                                 positiveButtonClickListener, negativeButtonTitle,
                                                 negativeButtonClickListener, cancelListener ->
                emitEvent(
                    AnalysisViewEvent.ShowAlertDialog(
                        message, positiveButtonTitle, positiveButtonClickListener,
                        negativeButtonTitle, negativeButtonClickListener, cancelListener
                    )
                )
            }
        ).handle(object : CompletableFuture.BiFun<Void, Throwable, Void> {
            override fun apply(aVoid: Void?, throwable: Throwable?): Void? {
                if (throwable != null) {
                    emitEvent(AnalysisViewEvent.NotifyDefaultPDFAppAlertDialogCancelled)
                } else {
                    showErrorIfAvailableAndAnalyzeDocument()
                }
                return null
            }
        })
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

    private fun showErrorIfAvailableAndAnalyzeDocument() {
        if (!documentAnalysisErrorMessage.isNullOrEmpty()) {
            val errorDetails = mutableMapOf<String, Any>()
            errorDetails[ERROR_DETAILS_MAP_KEY.MESSAGE] = documentAnalysisErrorMessage

            if (GiniCapture.hasInstance()) {
                val reviewScreenAnalysisError =
                    GiniCapture.getInstance().internal().reviewScreenAnalysisError
                if (reviewScreenAnalysisError != null) {
                    errorDetails[ERROR_DETAILS_MAP_KEY.ERROR_OBJECT] = reviewScreenAnalysisError
                    trackAnalysisScreenEvent(AnalysisScreenEvent.ERROR, errorDetails)
                }
            }

            emitEvent(
                AnalysisViewEvent.ShowErrorMessage(documentAnalysisErrorMessage, multiPageDocument)
            )
        } else {
            doAnalyzeDocument()
        }
    }

    @VisibleForTesting
    open fun doAnalyzeDocument() {
        setScanAnimationActive(true)
        showLoadingIndicator {
            setScanAnimationActive(false)
        }
        analysisInteractor.analyzeMultiPageDocument(multiPageDocument)
            .handle(object : CompletableFuture.BiFun<ResultHolder, Throwable, Void> {
                override fun apply(resultHolder: ResultHolder?, throwable: Throwable?): Void? {
                    setScanAnimationActive(false)
                    if (isStopped()) {
                        return null
                    }
                    if (throwable != null) {
                        handleAnalysisError(throwable)
                        return null
                    }
                    if (resultHolder == null) {
                        return null
                    }
                    handleAnalysisResult(resultHolder)
                    return null
                }
            })
    }

    private fun handleAnalysisResult(resultHolder: ResultHolder) {
        val remoteAnalyzedDocument = RemoteAnalyzedDocument(
            resultHolder.documentId,
            resultHolder.documentFileName
        )
        val result = resultHolder.result
        var shouldClearImageCaches = true
        when (result) {
            AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS -> {
                mAnalysisCompleted = true
                lastAnalyzedDocumentProvider.update(remoteAnalyzedDocument)
                try {
                    attachDocToTransactionDialogProvider.update(remoteAnalyzedDocument)
                } catch (ignored: Exception) {
                }
                proceedSuccessNoExtractions()
            }

            AnalysisInteractor.Result.SUCCESS_WITH_EXTRACTIONS -> {
                mAnalysisCompleted = true
                lastAnalyzedDocumentProvider.update(remoteAnalyzedDocument)
                try {
                    attachDocToTransactionDialogProvider.update(remoteAnalyzedDocument)
                } catch (ignored: Exception) {
                }

                if (resultHolder.extractions.isEmpty() && !isCxMode()) {
                    proceedSuccessNoExtractions()
                } else if (isCxEmptyExtractions(resultHolder)) {
                    proceedSuccessNoExtractions()
                } else if (shouldShowAlreadyPaidInvoiceWarning(resultHolder)) {
                    successResultHolder = resultHolder
                    shouldClearImageCaches = false
                    showAlreadyPaidHint(isSavingInvoicesInProgress, resultHolder)
                } else if (shouldShowPaymentDueHint(resultHolder)) {
                    successResultHolder = resultHolder
                    shouldClearImageCaches = false
                    showPaymentDueHint(
                        resultHolder,
                        extractPaymentDueDateFromExtraction(resultHolder),
                        isSavingInvoicesInProgress
                    )
                } else {
                    successResultHolder = resultHolder
                    shouldClearImageCaches = false
                    proceedWithExtractionsWhenEducationFinished(
                        resultHolder, isSavingInvoicesInProgress
                    )
                }
            }

            AnalysisInteractor.Result.NO_NETWORK_SERVICE -> Unit

            else -> throw UnsupportedOperationException(
                "Unknown AnalysisInteractor result: $result"
            )
        }
        if (result != AnalysisInteractor.Result.NO_NETWORK_SERVICE
            && shouldClearImageCaches
        ) {
            clearSavedImages()
        }
    }

    private fun handleAnalysisError(throwable: Throwable) {
        val errorDetails = mutableMapOf<String, Any?>()
        errorDetails[ERROR_DETAILS_MAP_KEY.MESSAGE] = throwable.message
        errorDetails[ERROR_DETAILS_MAP_KEY.ERROR_OBJECT] = throwable
        trackAnalysisScreenEvent(AnalysisScreenEvent.ERROR, errorDetails)

        val failureException = FailureException.tryCastFromCompletableFutureThrowable(throwable)
        val errorType = failureException?.errorType ?: ErrorType.GENERAL
        emitEvent(AnalysisViewEvent.ShowErrorType(errorType, multiPageDocument))
    }

    private fun isCxMode(): Boolean {
        return GiniCapture.hasInstance() &&
                GiniCapture.getInstance().productTag is ProductTag.CxExtractions
    }

    private fun isCxEmptyExtractions(resultHolder: ResultHolder): Boolean {
        if (!isCxMode()) {
            return false
        }
        val cbp = resultHolder.compoundExtractions[CROSS_BORDER_PAYMENT_KEY]
        return cbp == null || cbp.specificExtractionMaps.isEmpty()
    }

    private fun proceedSuccessNoExtractions() {
        doWhenEducationFinished {
            EventTrackingHelper.trackAnalysisScreenEvent(AnalysisScreenEvent.NO_RESULTS)
            emitEvent(AnalysisViewEvent.NotifyProceedToNoExtractionsScreen(multiPageDocument))
        }
    }

    /**
     * Continues the invoice extraction flow depending on whether the education screen
     * has already been shown.
     *
     * If `isSavingInvoicesInProgress` is true, it means the education step was already
     * completed and only the local invoice saving process is pending. In that case,
     * saving resumes immediately and the result will be returned to the customer afterward.
     *
     * If false, the education screen has not been shown yet. After education finishes,
     * the local invoice saving process will start.
     */
    @VisibleForTesting
    fun proceedWithExtractionsWhenEducationFinished(
        resultHolder: ResultHolder,
        isSavingInvoicesInProgress: Boolean
    ) {
        if (isSavingInvoicesInProgress) {
            handleSaveInvoicesLocally(true, resultHolder)
        } else {
            doWhenEducationFinished {
                handleSaveInvoicesLocally(false, resultHolder)
            }
        }
    }

    @VisibleForTesting
    fun proceedWithExtractions(resultHolder: ResultHolder) {
        emitEvent(
            AnalysisViewEvent.NotifyExtractionsAvailable(
                getMapOrEmpty(resultHolder.extractions),
                getMapOrEmpty(resultHolder.compoundExtractions),
                getListOrEmpty(resultHolder.returnReasons)
            )
        )
    }

    private fun showAlreadyPaidHint(
        isSavingInvoicesInProgress: Boolean,
        resultHolder: ResultHolder
    ) {
        if (isSavingInvoicesInProgress) {
            handleSaveInvoicesLocally(true, resultHolder)
        } else {
            doWhenEducationFinished {
                emitEvent(
                    AnalysisViewEvent.ShowAlreadyPaidWarning(
                        WarningType.DOCUMENT_MARKED_AS_PAID,
                        Runnable { handleSaveInvoicesLocally(false, resultHolder) }
                    )
                )
            }
        }
    }

    private fun handleSaveInvoicesLocally(
        isSavingInvoicesInProgress: Boolean,
        resultHolder: ResultHolder
    ) {
        if (!isInvoiceSavingEnabled || isSavingInvoicesInProgress) {
            clearSavedImagesAndProceed(resultHolder)
        } else {
            emitEvent(AnalysisViewEvent.ProcessInvoiceSaving)
        }
    }

    private fun clearSavedImagesAndProceed(resultHolder: ResultHolder) {
        ImageDiskStore.clear(app)
        proceedWithExtractions(resultHolder)
    }

    private fun showPaymentDueHint(
        resultHolder: ResultHolder,
        dueDate: String,
        isSavingInvoicesInProgress: Boolean
    ) {
        if (isSavingInvoicesInProgress) {
            handleSaveInvoicesLocally(true, resultHolder)
        } else {
            doWhenEducationFinished {
                emitEvent(
                    AnalysisViewEvent.ShowPaymentDueHint(
                        dueDate,
                        PaymentDueHintDismissListener {
                            handleSaveInvoicesLocally(false, resultHolder)
                        }
                    )
                )
            }
        }
    }

    private fun getInvoiceEducationType(): InvoiceEducationType? {
        runBlocking {
            invoiceEducationType =
                runCatching { getInvoiceEducationTypeUseCase.execute() }.getOrNull()
        }
        return invoiceEducationType
    }

    private fun showLoadingIndicator(
        onEducationFlowTriggered: () -> Unit
    ) = runBlocking {
        if (getInvoiceEducationType() != null) {
            emitEvent(
                AnalysisViewEvent.ShowEducation(
                    EducationCompleteListener {
                        runBlocking { incrementInvoiceRecognizedCounterUseCase.execute() }
                        educationMutex.unlock()
                    }
                )
            )
            educationMutex.lock()
            onEducationFlowTriggered()
        }
    }

    /**
     * Releases the education mutex so that pending post-analysis navigation may continue.
     */
    fun releaseMutexForEducation() {
        if (educationMutex.isLocked) educationMutex.unlock()
    }

    private fun doWhenEducationFinished(action: () -> Unit) {
        scope.launch {
            educationMutex.withLock {
                withContext(Dispatchers.Main) {
                    action()
                }
            }
        }
    }

    fun updateInvoiceSavingState(isInProgress: Boolean) {
        isSavingInvoicesInProgress = isInProgress
    }

    /**
     * Resumes the interrupted processing flow after the user selects a folder via SAF.
     *
     * After a configuration change `successResultHolder` may be `null`.
     * In that case the flow is restarted by calling [onStart], and the rest of the
     * flow is handled by [proceedWithExtractionsWhenEducationFinished].
     * So, this method must not attempt to resume.
     * When `successResultHolder` is non-null, this method clears saved images and
     * continues processing.
     */
    fun resumeInterruptedFlow() {
        val resultHolder = successResultHolder ?: return
        clearSavedImagesAndProceed(resultHolder)
    }

    /**
     * We only have to add the files which are captured from camera, That's why we have to filter
     * out the files which are imported via picker or they are from "open with" flow.
     */
    fun assembleMultiPageDocumentUris(): List<Uri> {
        val documents = multiPageDocument.documents ?: return emptyList()

        return documents
            .filter { doc ->
                doc.importMethod != Document.ImportMethod.PICKER &&
                        doc.importMethod != Document.ImportMethod.OPEN_WITH
            }
            .mapNotNull { doc -> doc.uri }
    }

    @VisibleForTesting
    fun isRAOrSkontoIncludedInExtractions(resultHolder: ResultHolder): Boolean {
        val bankSDKProperties: BankSDKProperties? =
            bankSDKBridge?.getBankSDKProperties(
                ResultHolder.toCaptureResult(resultHolder)
            )
        bankSDKProperties?.let {
            val isSkontoEnabled = bankSDKProperties.isSkontoSDKFlagEnabled &&
                    bankSDKProperties.isSkontoExtractionsValid

            val isReturnAssistantEnabled = bankSDKProperties.isReturnAssistantSDKFlagEnabled &&
                    bankSDKProperties.isReturnAssistantExtractionsValid

            if (isSkontoEnabled || isReturnAssistantEnabled) {
                return true
            }
        }

        return false
    }

    private fun shouldShowAlreadyPaidInvoiceWarning(resultHolder: ResultHolder): Boolean {
        if (isCxMode()) {
            return false
        }
        // Feature flags / config
        val alreadyPaidHintClientFlagEnabled = alreadyPaidHintEnabledUseCase.invoke()

        val alreadyPaidHintSDKFlag =
            GiniCapture.hasInstance() && GiniCapture.getInstance().isAlreadyPaidHintEnabled

        if (!alreadyPaidHintClientFlagEnabled || !alreadyPaidHintSDKFlag) {
            return false
        }

        // Payment state
        val state = extractPaymentState(resultHolder.extractions)
        return state.isPaid
    }

    private fun shouldShowPaymentDueHint(resultHolder: ResultHolder): Boolean {
        val paymentDueHintClientFlagEnabled = paymentDueHintEnabledUseCase.invoke()

        val paymentDueHintSDKFlag =
            GiniCapture.hasInstance() && GiniCapture.getInstance().isPaymentDueHintEnabled

        if (isCxMode()) {
            return false
        }

        if (isRAOrSkontoIncludedInExtractions(resultHolder)) {
            return false
        }

        if (!paymentDueHintClientFlagEnabled || !paymentDueHintSDKFlag) {
            return false
        }

        val paymentDueDate = extractPaymentDueDateFromExtraction(resultHolder)
        if (paymentDueDate.isEmpty()) {
            return false
        }

        if (calculateRemainingDays(paymentDueDate) <
            GiniCapture.getInstance().paymentDueHintThresholdDays
        ) {
            return false
        }

        val extractions = resultHolder.extractions
        // Payment state
        val state = extractPaymentState(extractions)

        return state.toBePaid()
    }

    private fun extractPaymentDueDateFromExtraction(resultHolder: ResultHolder): String {
        return resultHolder.extractions[EXTRACTION_PAYMENT_DUE_DATE]?.value ?: ""
    }

    private fun calculateRemainingDays(paymentDueDate: String): Int {
        return try {
            val today = LocalDate.now()
            val dueDate = LocalDate.parse(paymentDueDate)

            ChronoUnit.DAYS.between(today, dueDate).toInt()
        } catch (e: DateTimeParseException) {
            LOG.error("Failed to parse payment due date: $paymentDueDate", e)
            0
        }
    }

    // extracts the payment state from extractions
    private fun extractPaymentState(
        extractions: Map<String, GiniCaptureSpecificExtraction>
    ): WarningPaymentState {
        val paymentStateValue = extractions[EXTRACTION_PAYMENT_STATE]?.value
        return WarningPaymentState.from(paymentStateValue)
    }

    private fun setScanAnimationActive(active: Boolean) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mutableScanAnimationActive.value = active
        } else {
            mutableScanAnimationActive.postValue(active)
        }
    }

    private fun emitEvent(event: AnalysisViewEvent) {
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

    companion object {

        @VisibleForTesting
        const val PARCELABLE_MEMORY_CACHE_TAG = "ANALYSIS_FRAGMENT"

        private const val EXTRACTION_PAYMENT_STATE = "paymentState"
        private const val EXTRACTION_PAYMENT_DUE_DATE = "paymentDueDate"

        @VisibleForTesting
        const val CROSS_BORDER_PAYMENT_KEY = "crossBorderPayment"

        private val LOG: Logger = LoggerFactory.getLogger(AnalysisViewModel::class.java)
    }
}
