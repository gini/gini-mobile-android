package net.gini.android.capture.analysis

import android.app.Application
import android.content.DialogInterface
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.analysis.paymentDueHint.PaymentDueHintDismissListener
import net.gini.android.capture.paymentHints.GetAlreadyPaidHintEnabledUseCase
import net.gini.android.capture.paymentHints.GetPaymentDueHintEnabledUseCase
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY
import net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Collections
import java.util.Random
import jersey.repackaged.jsr166e.CompletableFuture

/**
 * Internal use only.
 *
 * ViewModel of the Analysis screen. Owns the UI state and the document analysis business logic
 * which used to live in the Analysis screen MVP presenter. The view layer observes [uiState] for
 * renderable state and [events] for one-shot effects (navigation, dialogs, listener
 * notifications).
 */
internal open class AnalysisViewModel(
    private val app: Application,
    document: Document,
    private val documentAnalysisErrorMessage: String?,
    private val isInvoiceSavingEnabled: Boolean,
    private val analysisInteractor: AnalysisInteractor = AnalysisInteractor(app)
) : ViewModel() {

    val alreadyPaidHintEnabledUseCase:
            GetAlreadyPaidHintEnabledUseCase by getGiniCaptureKoin().inject()

    val paymentDueHintEnabledUseCase:
            GetPaymentDueHintEnabledUseCase by getGiniCaptureKoin().inject()

    val lastAnalyzedDocumentProvider: LastAnalyzedDocumentProvider
            by getGiniCaptureKoin().inject()

    val attachDocToTransactionDialogProvider: AttachedToTransactionDocumentProvider
            by getGiniCaptureKoin().inject()

    private val getInvoiceEducationTypeUseCase: GetInvoiceEducationTypeUseCase
            by getGiniCaptureKoin().inject()

    private val incrementInvoiceRecognizedCounterUseCase: IncrementInvoiceRecognizedCounterUseCase
            by getGiniCaptureKoin().inject()

    private val _uiState = MutableStateFlow(initialUiState(isInvoiceSavingEnabled))
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AnalysisEvent>(extraBufferCapacity = EVENT_BUFFER_SIZE)
    val events: SharedFlow<AnalysisEvent> = _events.asSharedFlow()

    val multiPageDocument: GiniCaptureMultiPageDocument<GiniCaptureDocument,
            GiniCaptureDocumentError> = asMultiPageDocument(document)

    val hints: List<AnalysisHint> = generateRandomHintsList()

    var bankSDKBridge: BankSDKBridge? = null

    @VisibleForTesting
    @JvmField
    var mDocumentRenderer: DocumentRenderer? = null

    @get:VisibleForTesting
    var isStopped: Boolean = false
        private set

    private var analysisCompleted = false
    private var isSavingInvoicesInProgress = false
    private var successResultHolder: ResultHolder? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val educationMutex = Mutex()
    private var invoiceEducationType: InvoiceEducationType? = null

    init {
        // Tag the documents to be able to clean up the automatically parcelled data
        tagDocumentsForParcelableMemoryCache(document, multiPageDocument)
    }

    open fun start() {
        isStopped = false
        checkGiniCaptureInstance()
        if (multiPageDocument.type != Document.Type.XML &&
            multiPageDocument.type != Document.Type.XML_MULTI_PAGE
        ) {
            createDocumentRenderer()
        }
        clearParcelableMemoryCache()
        _uiState.update { it.copy(scanAnimationVisible = true) }
        loadDocumentData()
        showHintsForImage()
    }

    open fun stop() {
        isStopped = true
        job.cancel()
        stopScanAnimation()
        if (!analysisCompleted) {
            deleteUploadedDocuments()
        } else if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.clear()
        }
    }

    open fun finish() {
        clearParcelableMemoryCache()
    }

    override fun onCleared() {
        job.cancel()
    }

    /**
     * Called by the view layer once the view has been laid out after the document data was
     * loaded. Continues the flow which used to be driven by the presenter through
     * `View.waitForViewLayout()`.
     */
    open fun onViewLayoutFinished(previewSize: Size) {
        LOG.debug("View layout finished")
        showPdfInfoForPdfDocument()
        showDocument(previewSize)
        analyzeDocument()
    }

    open fun updateInvoiceSavingState(isInProgress: Boolean) {
        isSavingInvoicesInProgress = isInProgress
    }

    /**
     * Resumes the interrupted processing flow after the user selects a folder via SAF.
     *
     * After process death [successResultHolder] will be `null`. In that case the flow is
     * restarted by calling [start], and the rest of the flow is handled by
     * [proceedWithExtractions]. So, this method must not attempt to resume.
     * When [successResultHolder] is non-null, this method clears saved images and
     * continues processing.
     */
    open fun resumeInterruptedFlow() {
        val resultHolder = successResultHolder ?: return
        clearSavedImagesAndProceed(resultHolder)
    }

    /**
     * We only have to add the files which are captured from camera. That's why we have to filter
     * out the files which are imported via picker or are from the "open with" flow.
     */
    open fun assembleMultiPageDocumentUris(): List<Uri> {
        return multiPageDocument.documents.orEmpty()
            .filter {
                it.importMethod != Document.ImportMethod.PICKER &&
                        it.importMethod != Document.ImportMethod.OPEN_WITH
            }
            .mapNotNull { it.uri }
    }

    /**
     * Called by the view layer when the education UI has finished.
     */
    open fun onEducationCompleted() {
        runBlocking { incrementInvoiceRecognizedCounterUseCase.execute() }
        if (educationMutex.isLocked) educationMutex.unlock()
    }

    // region document loading and rendering

    private fun loadDocumentData() {
        LOG.debug("Loading document data")
        multiPageDocument.loadData(app, object : AsyncCallback<ByteArray, Exception> {
            override fun onSuccess(result: ByteArray?) {
                LOG.debug("Document data loaded")
                if (isStopped) {
                    return
                }
                emitEvent(AnalysisEvent.WaitForViewLayout)
            }

            override fun onError(exception: Exception?) {
                LOG.error("Failed to load document data", exception)
                if (isStopped) {
                    return
                }
                ErrorLogger.log(
                    ErrorLog(description = "Failed to load document data", exception = exception)
                )
                emitEvent(
                    AnalysisEvent.NotifyError(
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

    @VisibleForTesting
    open fun createDocumentRenderer() {
        val documentToRender = firstDocument
        if (documentToRender != null) {
            mDocumentRenderer = DocumentRendererFactory.fromDocument(documentToRender)
        }
    }

    private fun showDocument(previewSize: Size) {
        LOG.debug("Rendering the document")
        val documentRenderer = mDocumentRenderer ?: return
        documentRenderer.toBitmap(app, previewSize) { bitmap, rotationForDisplay ->
            LOG.debug("Document rendered")
            if (isStopped) {
                return@toBitmap
            }
            if (multiPageDocument.type == Document.Type.IMAGE_MULTI_PAGE ||
                multiPageDocument.type == Document.Type.IMAGE
            ) {
                return@toBitmap
            }
            _uiState.update {
                it.copy(documentRender = DocumentRender(bitmap, rotationForDisplay))
            }
        }
    }

    private fun showPdfInfoForPdfDocument() {
        val documentToRender = firstDocument
        if (documentToRender is PdfDocument) {
            _uiState.update { it.copy(pdfInfoPanelVisible = true) }
            val filename = getPdfFilename(documentToRender)
            if (filename != null) {
                _uiState.update { it.copy(pdfTitle = filename) }
            }
        }
    }

    @VisibleForTesting
    open fun getPdfFilename(pdfDocument: PdfDocument): String? = pdfDocument.filename

    private fun showHintsForImage() {
        val invoiceEducationType = getInvoiceEducationType()
        if (firstDocument?.type == Document.Type.IMAGE && invoiceEducationType == null) {
            _uiState.update { it.copy(hints = hints) }
        }
    }

    private val firstDocument: GiniCaptureDocument?
        get() = multiPageDocument.documents.firstOrNull()

    // endregion

    // region analysis

    @VisibleForTesting
    open fun analyzeDocument() {
        val showAlertCallback = FileImportHelper.ShowAlertCallback {
                message, positiveButtonTitle, positiveButtonClickListener,
                negativeButtonTitle, negativeButtonClickListener, cancelListener ->
            emitEvent(
                AnalysisEvent.ShowAlertDialog(
                    message, positiveButtonTitle, positiveButtonClickListener,
                    negativeButtonTitle, negativeButtonClickListener, cancelListener
                )
            )
        }
        showAlertIfOpenWithDocumentAndAppIsDefault(multiPageDocument, showAlertCallback)
            .handle { _, throwable ->
                if (throwable != null) {
                    emitEvent(AnalysisEvent.NotifyPdfAlertDialogCancelled)
                } else {
                    showErrorIfAvailableAndAnalyzeDocument()
                }
                null
            }
    }

    @VisibleForTesting
    open fun showAlertIfOpenWithDocumentAndAppIsDefault(
        document: GiniCaptureDocument,
        showAlertCallback: FileImportHelper.ShowAlertCallback
    ): CompletableFuture<Void> {
        return FileImportHelper.showAlertIfOpenWithDocumentAndAppIsDefault(
            app, document, showAlertCallback
        ) { emitEvent(AnalysisEvent.OpenApplicationDetailsSettings) }
    }

    private fun showErrorIfAvailableAndAnalyzeDocument() {
        if (!documentAnalysisErrorMessage.isNullOrEmpty()) {
            val errorDetails = mutableMapOf<String, Any>(
                ERROR_DETAILS_MAP_KEY.MESSAGE to documentAnalysisErrorMessage
            )
            if (GiniCapture.hasInstance()) {
                val reviewScreenAnalysisError =
                    GiniCapture.getInstance().internal().reviewScreenAnalysisError
                if (reviewScreenAnalysisError != null) {
                    errorDetails[ERROR_DETAILS_MAP_KEY.ERROR_OBJECT] = reviewScreenAnalysisError
                    trackAnalysisScreenEvent(AnalysisScreenEvent.ERROR, errorDetails)
                }
            }
            emitEvent(
                AnalysisEvent.ShowErrorMessage(documentAnalysisErrorMessage, multiPageDocument)
            )
        } else {
            doAnalyzeDocument()
        }
    }

    @VisibleForTesting
    open fun doAnalyzeDocument() {
        startScanAnimation()
        showLoadingIndicatorForEducation {
            stopScanAnimation()
        }
        analysisInteractor.analyzeMultiPageDocument(multiPageDocument)
            .handle { resultHolder: ResultHolder?, throwable: Throwable? ->
                stopScanAnimation()
                if (isStopped) {
                    return@handle null
                }
                if (throwable != null) {
                    handleAnalysisError(throwable)
                    return@handle null
                }
                requireNotNull(resultHolder)
                val remoteAnalyzedDocument = RemoteAnalyzedDocument(
                    resultHolder.documentId,
                    resultHolder.documentFileName
                )
                val result = resultHolder.result
                var shouldClearImageCaches = true
                when (result) {
                    AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS -> {
                        analysisCompleted = true
                        lastAnalyzedDocumentProvider.update(remoteAnalyzedDocument)
                        runCatching {
                            attachDocToTransactionDialogProvider.update(remoteAnalyzedDocument)
                        }
                        proceedSuccessNoExtractions()
                    }

                    AnalysisInteractor.Result.SUCCESS_WITH_EXTRACTIONS -> {
                        analysisCompleted = true
                        lastAnalyzedDocumentProvider.update(remoteAnalyzedDocument)
                        runCatching {
                            attachDocToTransactionDialogProvider.update(remoteAnalyzedDocument)
                        }
                        if (resultHolder.extractions.isEmpty() && !isCxMode()) {
                            proceedSuccessNoExtractions()
                        } else if (isCxEmptyExtractions(resultHolder)) {
                            proceedSuccessNoExtractions()
                        } else if (shouldShowAlreadyPaidInvoiceWarning(resultHolder)) {
                            successResultHolder = resultHolder
                            shouldClearImageCaches = false
                            showAlreadyPaidHint(resultHolder)
                        } else if (shouldShowPaymentDueHint(resultHolder)) {
                            successResultHolder = resultHolder
                            shouldClearImageCaches = false
                            showPaymentDueHint(
                                resultHolder,
                                extractPaymentDueDateFromExtraction(resultHolder)
                            )
                        } else {
                            successResultHolder = resultHolder
                            shouldClearImageCaches = false
                            proceedWithExtractionsWhenEducationFinished(resultHolder)
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
                null
            }
            .handle { _, throwable: Throwable? ->
                // The result of the previous handle stage is not observed anywhere, so log
                // errors thrown while handling the analysis result instead of swallowing them
                if (throwable != null) {
                    LOG.error("Failed to handle the analysis result", throwable)
                }
                null
            }
    }

    private fun handleAnalysisError(throwable: Throwable) {
        val errorDetails = mapOf(
            ERROR_DETAILS_MAP_KEY.MESSAGE to throwable.message,
            ERROR_DETAILS_MAP_KEY.ERROR_OBJECT to throwable
        )
        trackAnalysisScreenEvent(AnalysisScreenEvent.ERROR, errorDetails)

        val failureException = FailureException.tryCastFromCompletableFutureThrowable(throwable)
        val errorType = failureException?.errorType ?: ErrorType.GENERAL
        emitEvent(AnalysisEvent.ShowErrorType(errorType, multiPageDocument))
    }

    private fun checkGiniCaptureInstance() {
        if (!GiniCapture.hasInstance()) {
            emitEvent(AnalysisEvent.ShowErrorType(ErrorType.GENERAL, multiPageDocument))
        }
    }

    // endregion

    // region payment hints

    fun isRAOrSkontoIncludedInExtractions(resultHolder: ResultHolder): Boolean {
        val bankSDKProperties: BankSDKProperties? =
            bankSDKBridge?.getBankSDKProperties(ResultHolder.toCaptureResult(resultHolder))
        bankSDKProperties?.let {
            val isSkontoEnabled = it.isSkontoSDKFlagEnabled && it.isSkontoExtractionsValid

            val isReturnAssistantEnabled = it.isReturnAssistantSDKFlagEnabled &&
                    it.isReturnAssistantExtractionsValid

            if (isSkontoEnabled || isReturnAssistantEnabled) {
                return true
            }
        }
        return false
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
        return extractPaymentState(resultHolder.extractions).isPaid
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

        // Payment state
        return extractPaymentState(resultHolder.extractions).toBePaid()
    }

    private fun extractPaymentDueDateFromExtraction(resultHolder: ResultHolder): String =
        resultHolder.extractions[EXTRACTION_PAYMENT_DUE_DATE]?.value ?: ""

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

    private fun showAlreadyPaidHint(resultHolder: ResultHolder) {
        if (isSavingInvoicesInProgress) {
            handleSaveInvoicesLocally(true, resultHolder)
        } else {
            doWhenEducationFinished {
                emitEvent(
                    AnalysisEvent.ShowAlreadyPaidWarning(
                        WarningType.DOCUMENT_MARKED_AS_PAID,
                        Runnable { handleSaveInvoicesLocally(false, resultHolder) }
                    )
                )
            }
        }
    }

    private fun showPaymentDueHint(resultHolder: ResultHolder, dueDate: String) {
        if (isSavingInvoicesInProgress) {
            handleSaveInvoicesLocally(true, resultHolder)
        } else {
            doWhenEducationFinished {
                emitEvent(
                    AnalysisEvent.ShowPaymentDueHint(
                        dueDate,
                        PaymentDueHintDismissListener {
                            handleSaveInvoicesLocally(false, resultHolder)
                        }
                    )
                )
            }
        }
    }

    // endregion

    // region proceeding with results

    private fun proceedSuccessNoExtractions() {
        doWhenEducationFinished {
            trackAnalysisScreenEvent(AnalysisScreenEvent.NO_RESULTS)
            emitEvent(AnalysisEvent.NotifyNoExtractions(multiPageDocument))
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
    private fun proceedWithExtractionsWhenEducationFinished(resultHolder: ResultHolder) {
        if (isSavingInvoicesInProgress) {
            handleSaveInvoicesLocally(true, resultHolder)
        } else {
            doWhenEducationFinished {
                handleSaveInvoicesLocally(false, resultHolder)
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
            emitEvent(AnalysisEvent.ProcessInvoiceSaving)
        }
    }

    private fun clearSavedImagesAndProceed(resultHolder: ResultHolder) {
        clearSavedImages()
        proceedWithExtractions(resultHolder)
    }

    @VisibleForTesting
    open fun proceedWithExtractions(resultHolder: ResultHolder) {
        emitEvent(
            AnalysisEvent.NotifyExtractionsAvailable(
                getMapOrEmpty(resultHolder.extractions),
                getMapOrEmpty(resultHolder.compoundExtractions),
                getListOrEmpty(resultHolder.returnReasons)
            )
        )
    }

    // endregion

    // region education

    fun getInvoiceEducationType(): InvoiceEducationType? {
        runBlocking {
            invoiceEducationType =
                runCatching { getInvoiceEducationTypeUseCase.execute() }.getOrNull()
        }
        return invoiceEducationType
    }

    private fun showLoadingIndicatorForEducation(
        onEducationFlowTriggered: () -> Unit
    ) = runBlocking {
        if (getInvoiceEducationType() != null) {
            // Lock before emitting so that an immediately completing education UI cannot
            // unlock the mutex before it was locked
            educationMutex.lock()
            emitEvent(AnalysisEvent.ShowEducation)
            onEducationFlowTriggered()
        }
    }

    @VisibleForTesting
    fun doWhenEducationFinished(action: () -> Unit) {
        scope.launch {
            educationMutex.withLock {
                withContext(Dispatchers.Main) {
                    action()
                }
            }
        }
    }

    // endregion

    // region housekeeping

    @VisibleForTesting
    open fun clearParcelableMemoryCache() {
        // Remove data from the memory cache. The data had been added when the document in the
        // arguments was automatically parcelled when the activity was stopped
        ParcelableMemoryCache.getInstance().removeEntriesWithTag(PARCELABLE_MEMORY_CACHE_TAG)
    }

    @VisibleForTesting
    open fun clearSavedImages() {
        ImageDiskStore.clear(app)
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

    // endregion

    private fun startScanAnimation() {
        _uiState.update { it.copy(scanAnimationVisible = true) }
    }

    private fun stopScanAnimation() {
        _uiState.update { it.copy(scanAnimationVisible = false) }
    }

    private fun emitEvent(event: AnalysisEvent) {
        if (!_events.tryEmit(event)) {
            LOG.error("Event buffer overflow, dropped event: {}", event)
        }
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
        multiPageDocument: GiniCaptureMultiPageDocument<GiniCaptureDocument,
                GiniCaptureDocumentError>
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
        } else {
            document as GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
        }
    }

    class Factory(
        private val app: Application,
        private val document: Document,
        private val documentAnalysisErrorMessage: String?,
        private val isInvoiceSavingEnabled: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnalysisViewModel(
                app,
                document,
                documentAnalysisErrorMessage,
                isInvoiceSavingEnabled
            ) as T
        }
    }

    companion object {
        @VisibleForTesting
        const val PARCELABLE_MEMORY_CACHE_TAG = "ANALYSIS_FRAGMENT"

        private const val EXTRACTION_PAYMENT_STATE = "paymentState"
        private const val EXTRACTION_PAYMENT_DUE_DATE = "paymentDueDate"

        @VisibleForTesting
        const val CROSS_BORDER_PAYMENT_KEY = "crossBorderPayment"

        private const val EVENT_BUFFER_SIZE = 64

        private val LOG: Logger = LoggerFactory.getLogger(AnalysisViewModel::class.java)

        @JvmStatic
        fun initialUiState(isSavingInvoicesLocallyEnabled: Boolean) =
            AnalysisUiState(isSavingInvoicesLocallyEnabled = isSavingInvoicesLocallyEnabled)
    }
}

/**
 * Internal use only.
 *
 * Renderable state of the Analysis screen.
 */
internal data class AnalysisUiState(
    val scanAnimationVisible: Boolean = false,
    val isSavingInvoicesLocallyEnabled: Boolean = false,
    val hints: List<AnalysisHint>? = null,
    val pdfInfoPanelVisible: Boolean = false,
    val pdfTitle: String? = null,
    val documentRender: DocumentRender? = null
)

/**
 * Internal use only.
 *
 * Result of rendering the document for preview.
 */
internal data class DocumentRender(
    val bitmap: Bitmap?,
    val rotationForDisplay: Int
)

/**
 * Internal use only.
 *
 * One-shot effects emitted by [AnalysisViewModel] and handled by the view layer.
 */
internal sealed class AnalysisEvent {

    /**
     * The document data was loaded; the view must wait for its layout and then call
     * [AnalysisViewModel.onViewLayoutFinished] with the preview size.
     */
    object WaitForViewLayout : AnalysisEvent()

    data class ShowAlertDialog(
        val message: String,
        val positiveButtonTitle: String,
        val positiveButtonClickListener: DialogInterface.OnClickListener,
        val negativeButtonTitle: String?,
        val negativeButtonClickListener: DialogInterface.OnClickListener?,
        val cancelListener: DialogInterface.OnCancelListener?
    ) : AnalysisEvent()

    data class ShowErrorMessage(val message: String, val document: Document) : AnalysisEvent()

    data class ShowErrorType(val errorType: ErrorType, val document: Document) : AnalysisEvent()

    data class ShowAlreadyPaidWarning(
        val warningType: WarningType,
        val onProceed: Runnable
    ) : AnalysisEvent()

    data class ShowPaymentDueHint(
        val dueDate: String,
        val dismissListener: PaymentDueHintDismissListener
    ) : AnalysisEvent()

    object ShowEducation : AnalysisEvent()

    object ProcessInvoiceSaving : AnalysisEvent()

    object OpenApplicationDetailsSettings : AnalysisEvent()

    data class NotifyError(val error: GiniCaptureError) : AnalysisEvent()

    data class NotifyExtractionsAvailable(
        val extractions: Map<String, GiniCaptureSpecificExtraction>,
        val compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
        val returnReasons: List<GiniCaptureReturnReason>
    ) : AnalysisEvent()

    data class NotifyNoExtractions(val document: Document) : AnalysisEvent()

    object NotifyPdfAlertDialogCancelled : AnalysisEvent()
}
