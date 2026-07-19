package net.gini.android.capture.camera

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jersey.repackaged.jsr166e.CompletableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.gini.android.capture.AsyncCallback
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.ImportImageFileUrisAsyncTask
import net.gini.android.capture.ImportedFileValidationException
import net.gini.android.capture.analysis.ConsumableEvent
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.document.DocumentFactory
import net.gini.android.capture.document.GiniCaptureDocument
import net.gini.android.capture.document.GiniCaptureMultiPageDocument
import net.gini.android.capture.Document.ImportMethod
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.document.QRCodeDocument
import net.gini.android.capture.education.GetEducationFeatureEnabledUseCase
import net.gini.android.capture.einvoice.GetEInvoiceFeatureEnabledUseCase
import net.gini.android.capture.error.ErrorType
import net.gini.android.capture.internal.fileimport.FileChooserResult
import net.gini.android.capture.internal.network.AnalysisNetworkRequestResult
import net.gini.android.capture.internal.network.FailureException
import net.gini.android.capture.internal.network.NetworkRequestResult
import net.gini.android.capture.internal.network.NetworkRequestsManager
import net.gini.android.capture.internal.qrcode.EPSPaymentParser
import net.gini.android.capture.internal.qrcode.PaymentQRCodeData
import net.gini.android.capture.internal.qreducation.GetQrEducationTypeUseCase
import net.gini.android.capture.internal.qreducation.IncrementQrCodeRecognizedCounterUseCase
import net.gini.android.capture.internal.qreducation.UpdateFlowTypeUseCase
import net.gini.android.capture.internal.qreducation.model.FlowType
import net.gini.android.capture.internal.util.DeviceHelper
import net.gini.android.capture.internal.util.FeatureConfiguration
import net.gini.android.capture.internal.util.FileImportValidator
import net.gini.android.capture.internal.util.LogSanitizer
import net.gini.android.capture.internal.util.MimeType
import net.gini.android.capture.logging.ErrorLog
import net.gini.android.capture.logging.ErrorLogger
import net.gini.android.capture.network.Error
import net.gini.android.capture.network.model.GiniCaptureExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.util.IntentHelper
import net.gini.android.capture.util.UriHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * ViewModel for the Camera screen.
 *
 * Contains the non-view-bound presentation logic of the former fragment-as-controller
 * implementation ([CameraFragmentImpl] and `CameraFragmentExtension`): document creation and
 * import handling, the QR code detected/processing state machine (incl. the QR code education
 * flow), multi-page state, camera permission outcome routing, the invalid file/multi-page limit
 * dialog state and the [CameraFragmentListener] notification decisions.
 *
 * View-bound concerns (CameraX controller wiring, preview surface, animations, view visibility)
 * remain in the fragment layer which executes the one-shot commands emitted via [events].
 *
 * Internal use only.
 */
internal open class CameraViewModel(
    private val app: Application
) : ViewModel() {

    /**
     * Whether the camera interface is hidden because a QR code popup or the QR code education
     * flow is being shown. Gates the QR code detection state machine and the interface
     * animations performed by the fragment layer.
     */
    @VisibleForTesting
    @JvmField
    var interfaceHidden: Boolean = false

    /**
     * The content of the last detected (unsupported) QR code. Used to debounce showing the
     * unsupported QR code popup for the same QR code.
     */
    @VisibleForTesting
    @JvmField
    var qrCodeContent: String? = null

    private var inMultiPageState: Boolean = false
    private var multiPageDocument: ImageMultiPageDocument? = null

    private var isGenericErrorShowing: Boolean = false
    private var currentGenericErrorMessage: String = ""
    private var genericErrorType: String = ""

    private var shouldSendUserAnalyticsTrackerForQrCodes: Boolean = true

    private var importUrisAsyncTask: ImportImageFileUrisAsyncTask? = null

    @VisibleForTesting
    @JvmField
    var userAnalyticsEventTracker: UserAnalyticsEventTracker? = null

    private val updateFlowTypeUseCase: UpdateFlowTypeUseCase by getGiniCaptureKoin().inject()
    private val getQrEducationTypeUseCase:
            GetQrEducationTypeUseCase by getGiniCaptureKoin().inject()
    private val incrementQrCodeRecognizedCounterUseCase:
            IncrementQrCodeRecognizedCounterUseCase by getGiniCaptureKoin().inject()
    private val getEInvoiceFeatureEnabledUseCase:
            GetEInvoiceFeatureEnabledUseCase by getGiniCaptureKoin().inject()
    private val getEducationFeatureEnabledUseCase:
            GetEducationFeatureEnabledUseCase by getGiniCaptureKoin().inject()

    private val educationMutex = Mutex()

    // One-shot commands are kept in a queue and consumers are notified via the [events] signal.
    // A queue is used (instead of putting the event into the LiveData value) so that no event is
    // lost when several events are emitted in quick succession or re-entrantly while a previous
    // event is being dispatched (LiveData coalesces values, including nested setValue calls).
    private val eventQueue = ArrayDeque<CameraViewEvent>()

    private val mutableEvents = MutableLiveData<ConsumableEvent<Unit>>()

    /**
     * Signals that one or more one-shot commands are available. Consumers must drain the pending
     * commands with [pollEvent] whenever an unhandled signal is received.
     */
    val events: LiveData<ConsumableEvent<Unit>> = mutableEvents

    /**
     * Returns the next pending one-shot command or `null` if there is none.
     */
    fun pollEvent(): CameraViewEvent? = synchronized(eventQueue) {
        eventQueue.removeFirstOrNull()
    }

    /**
     * Starts the screen logic. Must be called from the fragment's `onStart()` before the camera
     * is initialized: resets the QR education flow type and verifies that the
     * [GiniCapture] instance is available.
     */
    fun onStart() {
        updateFlowTypeUseCase.execute(null)
        if (!GiniCapture.hasInstance()) {
            emitEvent(CameraViewEvent.NavigateToError(ErrorType.GENERAL, multiPageDocument))
        }
    }

    /**
     * Routes the camera permission outcome: emits [CameraViewEvent.OpenCamera] when the
     * permission is granted or [CameraViewEvent.ShowNoPermissionView] otherwise.
     */
    fun checkCameraPermission() {
        if (isCameraPermissionGranted()) {
            emitEvent(CameraViewEvent.OpenCamera)
        } else {
            emitEvent(CameraViewEvent.ShowNoPermissionView)
        }
    }

    private fun isCameraPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    fun isInMultiPageState(): Boolean = inMultiPageState

    fun getMultiPageDocument(): ImageMultiPageDocument? = multiPageDocument

    /**
     * Restores the multi-page state saved in the fragment's instance state bundle.
     */
    fun restoreInMultiPageState(inMultiPageState: Boolean) {
        this.inMultiPageState = inMultiPageState
    }

    fun isGenericErrorShowing(): Boolean = isGenericErrorShowing

    fun getCurrentGenericErrorMessage(): String = currentGenericErrorMessage

    fun getGenericErrorType(): String = genericErrorType

    /**
     * Restores the invalid file/multi-page limit dialog state saved in the fragment's instance
     * state bundle and re-emits the corresponding dialog event if a dialog was showing.
     */
    fun restoreGenericErrorState(
        isShowing: Boolean,
        message: String,
        type: String
    ) {
        isGenericErrorShowing = isShowing
        currentGenericErrorMessage = message
        genericErrorType = type

        if (isShowing && type.isNotEmpty()) {
            if (type.equals(ERROR_TYPE_INVALID_FILE, ignoreCase = true) && message.isNotEmpty()) {
                showInvalidFileAlert(message)
            } else if (type.equals(ERROR_TYPE_MULTI_PAGE, ignoreCase = true)) {
                showMultiPageLimitError()
            }
        }
    }

    /**
     * Resets the invalid file/multi-page limit dialog state. Must be called by the fragment when
     * the dialog is dismissed or cancelled.
     */
    fun resetGenericDialogState() {
        currentGenericErrorMessage = ""
        isGenericErrorShowing = false
        genericErrorType = ""
    }

    /**
     * Synchronizes the multi-page state with the multi-page document kept in the
     * [GiniCapture] memory store and notifies the fragment about the resulting state.
     */
    fun initMultiPageDocument() {
        if (GiniCapture.hasInstance()) {
            val storedMultiPageDocument = GiniCapture.getInstance().internal()
                .imageMultiPageDocumentMemoryStore.multiPageDocument
            if (storedMultiPageDocument != null && storedMultiPageDocument.documents.size > 0) {
                multiPageDocument = storedMultiPageDocument
                inMultiPageState = true
                emitEvent(CameraViewEvent.UpdatePhotoThumbnail)
                emitEvent(CameraViewEvent.MultiPageStateChanged(true))
            } else {
                inMultiPageState = false
                multiPageDocument = null
                emitEvent(CameraViewEvent.MultiPageStateChanged(false))
            }
        }
    }

    // region QR code detection state machine

    /**
     * Handles a detected QR code. Must be called by the fragment layer with the current
     * visibility of the payment and unsupported QR code popups (view-bound state).
     *
     * @param paymentQRCodeData the parsed payment data or `null` for non-payment QR codes
     */
    fun onQRCodeDetected(
        paymentQRCodeData: PaymentQRCodeData?,
        qrCodeContent: String,
        paymentQRCodeDetectionInProgress: Boolean,
        unsupportedQRCodePopupShown: Boolean
    ) {
        if (interfaceHidden) {
            return
        }

        if (paymentQRCodeDetectionInProgress || unsupportedQRCodePopupShown) {
            return
        }

        emitEvent(CameraViewEvent.HideIbanDetected)

        if (this.qrCodeContent == null || this.qrCodeContent != qrCodeContent) {
            showQRCodeView(paymentQRCodeData, qrCodeContent)
        } else {
            showQRCodeViewWithDelay(paymentQRCodeData, qrCodeContent)
        }

        interfaceHidden = true
    }

    private fun showQRCodeViewWithDelay(data: PaymentQRCodeData?, qrCodeContent: String) {
        Handler(Looper.getMainLooper())
            .postDelayed({ showQRCodeView(data, qrCodeContent) }, SAME_QRCODE_DETECTED_POPUP_DELAY_MS)
    }

    private fun showQRCodeView(data: PaymentQRCodeData?, qrCodeContent: String) {
        if (data == null) {
            this.qrCodeContent = qrCodeContent
            emitEvent(CameraViewEvent.ShowUnsupportedQRCodePopup)
        } else {
            showQrCodePopup(data)
        }
    }

    /**
     * Shows the payment QR code popup or, when the QR code education flow has to be shown,
     * emits [CameraViewEvent.ShowQRCodeEducation] and proceeds with processing the payment
     * QR code while the education is displayed (former `CameraFragmentExtension.showQrCodePopup`).
     */
    private fun showQrCodePopup(data: PaymentQRCodeData) = runBlocking {
        updateFlowTypeUseCase.execute(FlowType.QrCode)
        val type = getQrEducationTypeUseCase.execute()
        if (type != null && getEducationFeatureEnabledUseCase.invoke()) {
            emitEvent(CameraViewEvent.ShowQRCodeEducation(type) {
                runBlocking {
                    incrementQrCodeRecognizedCounterUseCase.execute()
                    educationMutex.unlock()
                }
            })
            educationMutex.lock()
            handlePaymentQRCodeData(data)
        } else {
            emitEvent(CameraViewEvent.ShowPaymentQRCodePopup(data))
        }
    }

    /**
     * Must be called by the fragment when the unsupported QR code popup was actually shown,
     * so that the "QR code scanned" analytics event is sent (at most once).
     */
    fun onUnsupportedQRCodePopupShown() {
        sendQRCodeScannedEventToUserAnalytics(false)
    }

    /**
     * Must be called by the fragment when the unsupported QR code popup was hidden so that
     * subsequent QR codes can be detected again.
     */
    fun onUnsupportedQRCodePopupHidden() {
        qrCodeContent = null
        interfaceHidden = false
    }

    /**
     * Processes the payment QR code data: creates a [QRCodeDocument] and analyzes it, or
     * extracts the EPS payment URL, depending on the QR code format.
     */
    fun handlePaymentQRCodeData(paymentQRCodeData: PaymentQRCodeData) {
        when (paymentQRCodeData.format) {
            PaymentQRCodeData.Format.EPC069_12,
            PaymentQRCodeData.Format.BEZAHL_CODE,
            PaymentQRCodeData.Format.GINI_PAYMENT -> {
                val qrCodeDocument = QRCodeDocument.fromPaymentQRCodeData(paymentQRCodeData)
                sendQRCodeScannedEventToUserAnalytics(true)
                analyzeQRCode(qrCodeDocument)
            }

            PaymentQRCodeData.Format.EPS_PAYMENT -> {
                sendQRCodeScannedEventToUserAnalytics(true)
                handleEPSPaymentQRCode(paymentQRCodeData)
            }

            else -> {
                sendQRCodeScannedEventToUserAnalytics(false)
                LOG.error(
                    "Unknown payment QR Code format: {}",
                    LogSanitizer.sanitize(paymentQRCodeData)
                )
            }
        }
    }

    private fun handleEPSPaymentQRCode(paymentQRCodeData: PaymentQRCodeData) {
        val extraction = GiniCaptureExtraction(
            paymentQRCodeData.unparsedContent, EPSPaymentParser.EXTRACTION_ENTITY_NAME,
            null
        )
        val specificExtraction = GiniCaptureSpecificExtraction(
            EPSPaymentParser.EXTRACTION_ENTITY_NAME,
            paymentQRCodeData.unparsedContent,
            EPSPaymentParser.EXTRACTION_ENTITY_NAME,
            null,
            listOf(extraction)
        )
        onQrCodeRecognized(
            mapOf(EPSPaymentParser.EXTRACTION_ENTITY_NAME to specificExtraction)
        )
    }

    @VisibleForTesting
    open fun analyzeQRCode(qrCodeDocument: QRCodeDocument) {
        if (!GiniCapture.hasInstance()) {
            return
        }
        val networkRequestsManager =
            GiniCapture.getInstance().internal().networkRequestsManager ?: return
        emitEvent(CameraViewEvent.ShowActivityIndicator)
        networkRequestsManager
            .upload(app, qrCodeDocument)
            .handle(object :
                CompletableFuture.BiFun<NetworkRequestResult<GiniCaptureDocument>, Throwable,
                        NetworkRequestResult<GiniCaptureDocument>> {
                override fun apply(
                    requestResult: NetworkRequestResult<GiniCaptureDocument>?,
                    throwable: Throwable?
                ): NetworkRequestResult<GiniCaptureDocument>? {
                    if (throwable != null) {
                        emitEvent(CameraViewEvent.HideActivityIndicator)
                        if (!NetworkRequestsManager.isCancellation(throwable)) {
                            handleAnalysisError(throwable, qrCodeDocument)
                        }
                    }
                    return requestResult
                }
            })
            .thenCompose(object :
                CompletableFuture.Fun<NetworkRequestResult<GiniCaptureDocument>,
                        CompletableFuture<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument<*, *>>>> {
                override fun apply(
                    requestResult: NetworkRequestResult<GiniCaptureDocument>?
                ): CompletableFuture<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument<*, *>>> {
                    if (requestResult != null) {
                        val multiPageDocument = DocumentFactory.newMultiPageDocument(qrCodeDocument)
                        @Suppress("UNCHECKED_CAST")
                        return networkRequestsManager.analyze(multiPageDocument)
                                as CompletableFuture<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument<*, *>>>
                    }
                    return CompletableFuture.completedFuture(null)
                }
            })
            .handle(object :
                CompletableFuture.BiFun<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument<*, *>>,
                        Throwable, Void> {
                override fun apply(
                    requestResult: AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument<*, *>>?,
                    throwable: Throwable?
                ): Void? {
                    emitEvent(CameraViewEvent.HideActivityIndicator)
                    if (throwable != null && !NetworkRequestsManager.isCancellation(throwable)) {
                        handleAnalysisError(throwable, qrCodeDocument)
                    } else if (requestResult != null) {
                        emitEvent(CameraViewEvent.HidePaymentQRCodePopup)
                        if (requestResult.analysisResult.extractions.isEmpty()) {
                            emitEvent(CameraViewEvent.NavigateToNoResults(qrCodeDocument))
                            return null
                        }
                        onQrCodeRecognized(requestResult.analysisResult.extractions)
                    }
                    return null
                }
            })
    }

    private fun handleAnalysisError(throwable: Throwable, document: Document) {
        val failureException = FailureException.tryCastFromCompletableFutureThrowable(throwable)
        trackAnalysisScreenEvent(AnalysisScreenEvent.ERROR)
        if (failureException != null) {
            emitEvent(CameraViewEvent.ShowError(failureException.errorType, document))
        } else {
            emitEvent(CameraViewEvent.ShowError(ErrorType.GENERAL, document))
        }
    }

    /**
     * Notifies the [CameraFragmentListener] (through the fragment layer) about the extractions
     * after the QR code education flow finished (former
     * `CameraFragmentExtension.onQrCodeRecognized`).
     */
    fun onQrCodeRecognized(extractions: Map<String, GiniCaptureSpecificExtraction>) {
        emitEvent(CameraViewEvent.HideImageCorners)
        viewModelScope.launch(Dispatchers.IO) {
            educationMutex.withLock {
                emitEvent(CameraViewEvent.NotifyExtractionsAvailable(extractions))
            }
        }
    }

    private fun sendQRCodeScannedEventToUserAnalytics(validQRCode: Boolean) {
        if (shouldSendUserAnalyticsTrackerForQrCodes) {
            analyticsEventTracker()?.trackEvent(
                UserAnalyticsEvent.QR_CODE_SCANNED,
                setOf(
                    UserAnalyticsEventProperty.Screen(SCREEN_NAME),
                    UserAnalyticsEventProperty.QrCodeValid(validQRCode)
                )
            )
            shouldSendUserAnalyticsTrackerForQrCodes = false
        }
    }

    private fun analyticsEventTracker(): UserAnalyticsEventTracker? {
        if (userAnalyticsEventTracker == null) {
            userAnalyticsEventTracker = UserAnalytics.getAnalyticsEventTracker()
        }
        return userAnalyticsEventTracker
    }

    // endregion

    // region Document import

    /**
     * Handles the result of the file chooser. The [activity] is only used synchronously for
     * document creation and validation and is not retained.
     */
    fun handleFileChooserResult(result: FileChooserResult, activity: Activity) {
        when (result) {
            is FileChooserResult.FilesSelected ->
                importDocumentFromIntent(result.dataIntent, activity)

            is FileChooserResult.FilesSelectedUri ->
                importDocumentFromUriList(result.list, activity)

            is FileChooserResult.Error -> {
                val message = "Document import failed: " + result.error.message
                LOG.error(message)
                showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC)
            }

            is FileChooserResult.Cancelled -> {
                // No-op
            }
        }
    }

    @VisibleForTesting
    fun importDocumentFromIntent(data: Intent, activity: Activity) {
        if (IntentHelper.hasMultipleUris(data)) {
            val uris = IntentHelper.getUris(data)
            if (uris == null) {
                LOG.error("Document import failed: Intent has no Uris")
                showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC)
                return
            }
            handleMultiPageDocumentAndCallListener(activity, data, uris)
        } else {
            val uri = IntentHelper.getUri(data)
            if (uri == null) {
                LOG.error("Document import failed: Intent has no Uri")
                showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC)
                return
            }
            if (!UriHelper.isUriInputStreamAvailable(uri, activity)) {
                LOG.error("Document import failed: InputStream not available for the Uri")
                showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC)
                return
            }

            if (isImage(data, activity)) {
                handleMultiPageDocumentAndCallListener(activity, data, listOf(uri))
            } else {
                val fileSizeLimit: Int = if (GiniCapture.hasInstance()) {
                    GiniCapture.getInstance().importedFileSizeBytesLimit
                } else {
                    FileImportValidator.FILE_SIZE_LIMIT
                }
                val fileImportValidator = FileImportValidator(activity, fileSizeLimit)
                if (fileImportValidator.matchesCriteria(data, uri)) {
                    createSinglePageDocumentAndCallListener(data, activity)
                } else {
                    val error = fileImportValidator.error
                    if (error != null) {
                        val errorClass = Error(error)
                        val errorType = ErrorType.typeFromError(
                            errorClass,
                            getEInvoiceFeatureEnabledUseCase.invoke()
                        )
                        showGenericInvalidFileError(errorType)
                    }
                }
            }
        }
    }

    private fun importDocumentFromUriList(uriList: List<Uri>, activity: Activity) {
        handleMultiPageDocumentAndCallListener(activity, Intent(Intent.ACTION_PICK), uriList)
    }

    private fun isImage(data: Intent, activity: Activity): Boolean =
        IntentHelper.hasMimeTypeWithPrefix(data, activity, MimeType.IMAGE_PREFIX.asString())

    private fun createSinglePageDocumentAndCallListener(data: Intent, activity: Activity) {
        try {
            val document = DocumentFactory.newDocumentFromIntent(
                data,
                activity,
                DeviceHelper.getDeviceOrientation(activity),
                DeviceHelper.getDeviceType(activity),
                ImportMethod.PICKER
            )
            LOG.info("Document imported: {}", LogSanitizer.sanitize(document))
            requestClientDocumentCheck(document)
        } catch (e: IllegalArgumentException) {
            LOG.error("Failed to import selected document", e)
            showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC)
        }
    }

    /**
     * Asks the client (via [CameraFragmentListener.onCheckImportedDocument], forwarded by the
     * fragment layer) to check the imported document and routes the client's decision.
     */
    @VisibleForTesting
    fun requestClientDocumentCheck(document: GiniCaptureDocument) {
        emitEvent(CameraViewEvent.ShowActivityIndicator)
        LOG.debug("Requesting document check from client")
        emitEvent(CameraViewEvent.RequestCheckImportedDocument(document,
            object : CameraFragmentListener.DocumentCheckResultCallback {
                override fun documentAccepted() {
                    LOG.debug("Client accepted the document")
                    emitEvent(CameraViewEvent.HideActivityIndicator)
                    if (document.type == Document.Type.IMAGE_MULTI_PAGE) {
                        addToMultiPageDocumentMemoryStore(document as ImageMultiPageDocument)
                        emitEvent(CameraViewEvent.ProceedToMultiPageReview(true))
                    } else {
                        if (document.isReviewable) {
                            if (document.type == Document.Type.IMAGE &&
                                document is ImageDocument
                            ) {
                                val multiPageDocument = ImageMultiPageDocument(
                                    document.source, document.importMethod
                                )
                                addToMultiPageDocumentMemoryStore(multiPageDocument)
                                multiPageDocument.addDocument(document)
                                emitEvent(CameraViewEvent.ProceedToMultiPageReview(true))
                            }
                        } else {
                            emitEvent(CameraViewEvent.NavigateToAnalysis(document, ""))
                        }
                    }
                }

                override fun documentRejected(messageForUser: String) {
                    LOG.debug(
                        "Client rejected the document: {}",
                        LogSanitizer.sanitize(messageForUser)
                    )
                    emitEvent(CameraViewEvent.HideActivityIndicator)
                    showInvalidFileAlert(messageForUser)
                }
            }))
    }

    private fun addToMultiPageDocumentMemoryStore(multiPageDocument: ImageMultiPageDocument) {
        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal()
                .imageMultiPageDocumentMemoryStore
                .setMultiPageDocument(multiPageDocument)
        }
    }

    private fun handleMultiPageDocumentAndCallListener(
        context: Context,
        intent: Intent,
        uris: List<Uri>
    ) {
        emitEvent(CameraViewEvent.ShowActivityIndicator)
        importUrisAsyncTask?.cancel(true)
        if (!GiniCapture.hasInstance()) {
            LOG.error(
                "Cannot import multi-page document. GiniCapture instance not available. " +
                        "Create it with GiniCapture.newInstance()."
            )
            return
        }
        if (exceedsMultiPageLimit()) {
            emitEvent(CameraViewEvent.HideActivityIndicator)
            showMultiPageLimitError()
            return
        }

        importUrisAsyncTask = ImportImageFileUrisAsyncTask(
            context, intent, GiniCapture.getInstance(),
            Document.Source.newExternalSource(), ImportMethod.PICKER,
            object : AsyncCallback<ImageMultiPageDocument, ImportedFileValidationException> {
                override fun onSuccess(result: ImageMultiPageDocument) {
                    emitEvent(CameraViewEvent.HideActivityIndicator)
                    val importedMultiPageDocument: ImageMultiPageDocument
                    if (multiPageDocument == null) {
                        inMultiPageState = true
                        multiPageDocument = result
                        importedMultiPageDocument = result
                    } else {
                        importedMultiPageDocument = multiPageDocument!!
                        importedMultiPageDocument.addDocuments(result.documents)
                    }
                    if (importedMultiPageDocument.documents.isEmpty()) {
                        LOG.error("Document import failed: Intent did not contain images")
                        showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC)
                        multiPageDocument = null // NOPMD
                        inMultiPageState = false
                        return
                    }
                    LOG.info("Document imported: {}", importedMultiPageDocument)
                    emitEvent(CameraViewEvent.UpdatePhotoThumbnail)
                    requestClientDocumentCheck(importedMultiPageDocument)
                }

                override fun onError(exception: ImportedFileValidationException) {
                    LOG.error("Document import failed", exception)
                    emitEvent(CameraViewEvent.HideActivityIndicator)
                    val error = exception.validationError
                    if (error != null) {
                        val errorClass = Error(error)
                        val errorType = ErrorType.typeFromError(
                            errorClass,
                            getEInvoiceFeatureEnabledUseCase.invoke()
                        )
                        showGenericInvalidFileError(errorType)
                    }
                }

                override fun onCancelled() {
                    // No-op
                }
            })
        importUrisAsyncTask?.execute(*uris.toTypedArray())
    }

    /**
     * Cancels a running document import. Must be called from the fragment's `onDestroy()`
     * (matching the former fragment-as-controller behavior).
     */
    fun cancelImportUrisAsyncTask() {
        importUrisAsyncTask?.cancel(true)
    }

    fun exceedsMultiPageLimit(): Boolean =
        inMultiPageState && (multiPageDocument?.documents?.size ?: 0) >=
                FileImportValidator.DOCUMENT_PAGE_LIMIT

    // endregion

    // region Picture taking

    /**
     * Must be called when a picture was taken so that the QR education flow type is updated.
     */
    fun onPictureTaken() {
        updateFlowTypeUseCase.execute(FlowType.Photo)
    }

    /**
     * Adds the captured image to the multi-page document (creating it if necessary) and returns
     * how the image was integrated so that the fragment layer can update the thumbnail, restart
     * the camera preview and navigate exactly like the former implementation.
     */
    fun onImageCaptured(document: ImageDocument): CapturedImageOutcome {
        val currentMultiPageDocument = multiPageDocument
        if (inMultiPageState && currentMultiPageDocument != null) {
            currentMultiPageDocument.addDocument(document)
            return CapturedImageOutcome.MULTI_PAGE_ADDED
        }
        val newMultiPageDocument = ImageMultiPageDocument(
            Document.Source.newCameraSource(), ImportMethod.NONE
        )
        GiniCapture.getInstance().internal()
            .imageMultiPageDocumentMemoryStore
            .setMultiPageDocument(newMultiPageDocument)
        newMultiPageDocument.addDocument(document)
        return if (FeatureConfiguration.isMultiPageEnabled()) {
            inMultiPageState = true
            multiPageDocument = newMultiPageDocument
            CapturedImageOutcome.MULTI_PAGE_CREATED
        } else {
            CapturedImageOutcome.SINGLE_PAGE
        }
    }

    /**
     * Outcome of [onImageCaptured]; mirrors the branches of the former `onPictureTaken`.
     */
    enum class CapturedImageOutcome {
        /** The image was added to the existing multi-page document. */
        MULTI_PAGE_ADDED,

        /** A new multi-page document was created for the image (multi-page enabled). */
        MULTI_PAGE_CREATED,

        /** A new multi-page document was created for the image (multi-page disabled). */
        SINGLE_PAGE
    }

    // endregion

    // region Errors

    /**
     * Logs the error and notifies the [CameraFragmentListener] (through the fragment layer).
     */
    fun handleError(
        errorCode: GiniCaptureError.ErrorCode,
        message: String,
        throwable: Throwable?
    ) {
        ErrorLogger.log(ErrorLog(description = "$errorCode: $message", exception = throwable))
        var errorMessage = message
        if (throwable != null) {
            LOG.error(message, throwable)
            // Add error info to the message to help clients, if they don't have logging enabled
            errorMessage = errorMessage + ": " + throwable.message
        } else {
            LOG.error(message)
        }
        emitEvent(CameraViewEvent.NotifyError(GiniCaptureError(errorCode, errorMessage)))
    }

    private fun showGenericInvalidFileError(errorType: ErrorType) {
        val message = app.getString(errorType.titleTextResource)
        analyticsEventTracker()?.trackEvent(
            UserAnalyticsEvent.ERROR_DIALOG_SHOWN,
            setOf(
                UserAnalyticsEventProperty.Screen(SCREEN_NAME),
                UserAnalyticsEventProperty.ErrorMessage(message)
            )
        )
        LOG.error("Invalid document {}", message)
        showInvalidFileAlert(message)
    }

    private fun showInvalidFileAlert(message: String) {
        currentGenericErrorMessage = message
        isGenericErrorShowing = true
        genericErrorType = ERROR_TYPE_INVALID_FILE
        emitEvent(CameraViewEvent.ShowInvalidFileAlert(message))
    }

    /**
     * Marks the multi-page limit dialog as showing and emits
     * [CameraViewEvent.ShowMultiPageLimitError].
     */
    fun showMultiPageLimitError() {
        isGenericErrorShowing = true
        genericErrorType = ERROR_TYPE_MULTI_PAGE
        emitEvent(CameraViewEvent.ShowMultiPageLimitError)
    }

    // endregion

    override fun onCleared() {
        super.onCleared()
        importUrisAsyncTask?.cancel(true)
    }

    private fun emitEvent(event: CameraViewEvent) {
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
        const val SAME_QRCODE_DETECTED_POPUP_DELAY_MS = 1000L

        @VisibleForTesting
        const val ERROR_TYPE_MULTI_PAGE = "ERROR_TYPE_MULTI_PAGE"

        @VisibleForTesting
        const val ERROR_TYPE_INVALID_FILE = "ERROR_TYPE_INVALID_FILE"

        private val SCREEN_NAME: UserAnalyticsScreen = UserAnalyticsScreen.Camera

        private val LOG: Logger = LoggerFactory.getLogger(CameraViewModel::class.java)
    }
}
