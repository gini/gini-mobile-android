package net.gini.android.capture.analysis

import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import jersey.repackaged.jsr166e.CompletableFuture
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.gini.android.capture.AsyncCallback
import net.gini.android.capture.BankSDKBridge
import net.gini.android.capture.BankSDKProperties
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.ProductTag
import net.gini.android.capture.analysis.AnalysisViewModel.Companion.CROSS_BORDER_PAYMENT_KEY
import net.gini.android.capture.document.DocumentFactory
import net.gini.android.capture.document.GiniCaptureDocument
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.document.ImageDocumentFake
import net.gini.android.capture.document.PdfDocument
import net.gini.android.capture.document.PdfDocumentFake
import net.gini.android.capture.di.CaptureSdkIsolatedKoinContext
import net.gini.android.capture.internal.document.DocumentRenderer
import net.gini.android.capture.internal.document.ImageMultiPageDocumentMemoryStore
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.internal.util.FileImportHelper.ShowAlertCallback
import net.gini.android.capture.internal.util.Size
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import java.util.Collections
import java.util.concurrent.CancellationException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Unit tests for [AnalysisViewModel].
 *
 * Ported from the former Analysis screen MVP presenter tests. View interactions are asserted
 * through the [AnalysisViewModel.uiState] StateFlow and the [AnalysisViewModel.events]
 * SharedFlow instead of a mocked contract view.
 */
@RunWith(AndroidJUnit4::class)
class AnalysisViewModelTest {

    private lateinit var app: Application

    private val koinTestModule = module {
        single { GiniBankConfigurationProvider() }
    }

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        UserAnalytics.initialize(InstrumentationRegistry.getInstrumentation().context)
        CaptureSdkIsolatedKoinContext.koin.loadModules(listOf(koinTestModule))
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
        CaptureSdkIsolatedKoinContext.koin.unloadModules(listOf(koinTestModule))
    }

    // region helpers

    private fun createViewModel(
        document: Document,
        giniCapture: GiniCapture? = createGiniCaptureInstance(),
        bitmap: Bitmap? = null,
        rotationForDisplay: Int = 0,
        documentAnalysisErrorMessage: String? = null,
        analysisInteractor: AnalysisInteractor? = null
    ): AnalysisViewModel {
        if (giniCapture != null) {
            GiniCaptureHelper.setGiniCaptureInstance(giniCapture)
        }
        val documentRenderer = object : DocumentRenderer {
            override fun toBitmap(
                context: Context,
                targetSize: Size,
                callback: DocumentRenderer.Callback
            ) {
                callback.onBitmapReady(bitmap, rotationForDisplay)
            }

            override fun getPageCount(
                context: Context,
                asyncCallback: AsyncCallback<Int, Exception>
            ) {
                asyncCallback.onSuccess(0)
            }
        }
        val interactor = analysisInteractor ?: AnalysisInteractor(app)
        val viewModel = object : AnalysisViewModel(
            app, document, documentAnalysisErrorMessage, false, interactor
        ) {
            public override fun createDocumentRenderer() {
                mDocumentRenderer = documentRenderer
            }
        }
        viewModel.bankSDKBridge = mock()
        return viewModel
    }

    private fun createGiniCaptureInstance(): GiniCapture {
        GiniCapture.newInstance(InstrumentationRegistry.getInstrumentation().context)
            .setGiniCaptureNetworkService(mock())
            .build()
        return GiniCapture.getInstance()
    }

    private fun createGiniCaptureInstanceWithProductTag(productTag: ProductTag): GiniCapture {
        GiniCapture.newInstance(InstrumentationRegistry.getInstrumentation().context)
            .setGiniCaptureNetworkService(mock())
            .setProductTag(productTag)
            .build()
        return GiniCapture.getInstance()
    }

    private fun createViewModelWithEmptyImageDocument(): AnalysisViewModel {
        val document: GiniCaptureDocument = DocumentFactory.newEmptyImageDocument(
            Document.Source.newCameraSource(), Document.ImportMethod.NONE
        )
        document.data = ByteArray(42)
        return createViewModel(document)
    }

    private fun createViewModelWithAnalysisFuture(
        document: Document,
        giniCapture: GiniCapture? = createGiniCaptureInstance(),
        analysisFuture: CompletableFuture<AnalysisInteractor.ResultHolder>
    ): AnalysisViewModel {
        val analysisInteractor = mock<AnalysisInteractor> {
            on { analyzeMultiPageDocument(any()) } doReturn analysisFuture
        }
        return createViewModel(
            document,
            giniCapture = giniCapture,
            analysisInteractor = analysisInteractor
        )
    }

    /**
     * Collects the view model's one-shot events synchronously into a list. When [autoLayout] is
     * true, [AnalysisEvent.WaitForViewLayout] is answered with
     * [AnalysisViewModel.onViewLayoutFinished] like the view layer's binder would do.
     */
    private fun collectEvents(
        viewModel: AnalysisViewModel,
        autoLayout: Boolean = true,
        previewSize: Size = Size(0, 0)
    ): MutableList<AnalysisEvent> {
        val events = CopyOnWriteArrayList<AnalysisEvent>()
        CoroutineScope(Dispatchers.Unconfined).launch {
            viewModel.events.collect { event ->
                events.add(event)
                if (autoLayout && event is AnalysisEvent.WaitForViewLayout) {
                    viewModel.onViewLayoutFinished(previewSize)
                }
                if (event is AnalysisEvent.ShowEducation) {
                    // Complete the education UI right away like the view layer would do
                    viewModel.onEducationCompleted()
                }
            }
        }
        return events
    }

    /**
     * Records every [AnalysisUiState] emitted by the view model, so tests can assert
     * intermediate states (e.g. the scan animation was visible during analysis).
     */
    private fun collectUiStates(viewModel: AnalysisViewModel): MutableList<AnalysisUiState> {
        val states = CopyOnWriteArrayList<AnalysisUiState>()
        CoroutineScope(Dispatchers.Unconfined).launch {
            viewModel.uiState.collect { states.add(it) }
        }
        return states
    }

    private inline fun <reified T : AnalysisEvent> awaitEvent(
        events: List<AnalysisEvent>,
        timeoutMs: Long = 3000
    ): T = awaitEvent(events, T::class.java, timeoutMs)

    private fun <T : AnalysisEvent> awaitEvent(
        events: List<AnalysisEvent>,
        eventClass: Class<T>,
        timeoutMs: Long
    ): T {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            events.firstOrNull { eventClass.isInstance(it) }?.let {
                return eventClass.cast(it)
            }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        error("Event ${eventClass.simpleName} not received within $timeoutMs ms")
    }

    private inline fun <reified T : AnalysisEvent> assertNoEvent(events: List<AnalysisEvent>) {
        shadowOf(Looper.getMainLooper()).idle()
        Truth.assertThat(events.filterIsInstance<T>()).isEmpty()
    }

    // endregion

    @Test
    @Throws(Exception::class)
    fun should_convertSinglePageDocument_intoMultiPage() {
        // Given
        val document: GiniCaptureDocument = DocumentFactory.newEmptyImageDocument(
            Document.Source.newCameraSource(), Document.ImportMethod.NONE
        )

        // When
        val viewModel = createViewModel(document, null)

        // Then
        val documentInMultiPage = viewModel.multiPageDocument.documents[0]
        Truth.assertThat(documentInMultiPage).isEqualTo(document)
    }

    @Test
    @Throws(Exception::class)
    fun should_tagDocuments_forParcelableMemoryCache() {
        // Given
        val document: GiniCaptureDocument = DocumentFactory.newEmptyImageDocument(
            Document.Source.newCameraSource(), Document.ImportMethod.NONE
        )

        // When
        val viewModel = createViewModel(document, null)

        // Then
        Truth.assertThat(document.parcelableMemoryCacheTag)
            .isEqualTo(AnalysisViewModel.PARCELABLE_MEMORY_CACHE_TAG)
        Truth.assertThat(viewModel.multiPageDocument.parcelableMemoryCacheTag)
            .isEqualTo(AnalysisViewModel.PARCELABLE_MEMORY_CACHE_TAG)
    }

    @Test
    @Throws(Exception::class)
    fun should_generateHintsList_withRandomOrder() {
        // Given
        val viewModels: MutableList<AnalysisViewModel> = ArrayList()
        val nrOfViewModels = 5
        for (i in 0 until nrOfViewModels) {
            viewModels.add(createViewModelWithEmptyImageDocument())
        }

        // Then
        assertHaveDifferentHintOrders(viewModels)
    }

    private fun assertHaveDifferentHintOrders(viewModels: List<AnalysisViewModel>) {
        val hints1 = viewModels[0].hints
        var countSamePosition = 0
        for (i in hints1.indices) {
            for (j in viewModels.indices) {
                val lhs = viewModels[j]
                for (k in j + 1 until viewModels.size) {
                    val rhs = viewModels[k]
                    if (lhs.hints[i] == rhs.hints[i]) {
                        countSamePosition++
                    }
                }
            }
        }
        val nrOfComparisons = viewModels.size - 1
        val nrOfPairwiseComparisons = (nrOfComparisons / 2.0 * (nrOfComparisons + 1)).toInt()
        val samePositionCountIfSameOrder = nrOfPairwiseComparisons * hints1.size
        Truth.assertThat(countSamePosition).isLessThan(samePositionCountIfSameOrder)
    }

    @Test
    @Throws(Exception::class)
    fun should_clearParcelableMemoryCache_whenStarted() {
        // Given
        val viewModel = spy(createViewModelWithEmptyImageDocument())

        // When
        viewModel.start()

        // Then
        verify(viewModel).clearParcelableMemoryCache()
    }

    @Test
    @Throws(Exception::class)
    fun should_startScanAnimation_whenStarted() {
        // Given
        val viewModel = createViewModelWithEmptyImageDocument()

        // When
        viewModel.start()

        // Then
        Truth.assertThat(viewModel.uiState.value.scanAnimationVisible).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun should_loadDocumentData_whenStarted() {
        // Given
        val document: GiniCaptureDocument = spy(
            DocumentFactory.newEmptyImageDocument(
                Document.Source.newCameraSource(), Document.ImportMethod.NONE
            )
        )
        val viewModel = createViewModel(document, null)

        // When
        viewModel.start()

        // Then
        verify(document).loadData(eq(app), any())
    }

    @Test
    @Throws(Exception::class)
    fun should_showHints_forImageDocument() {
        // Given
        val viewModel = createViewModelWithEmptyImageDocument()

        // When
        viewModel.start()

        // Then
        Truth.assertThat(viewModel.uiState.value.hints).isEqualTo(viewModel.hints)
    }

    @Test
    @Throws(Exception::class)
    fun should_notShowHints_forNonImageDocument() {
        // Given
        val pdfDocument = mock<PdfDocument>()
        whenever(pdfDocument.type).thenReturn(Document.Type.PDF)
        val viewModel = createViewModel(pdfDocument, null)

        // When
        viewModel.start()

        // Then
        Truth.assertThat(viewModel.uiState.value.hints).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun should_returnError_throughEvents_whenDocumentLoadingFailed() {
        // Given
        val imageDocument = ImageDocumentFake()
        imageDocument.failWithException = RuntimeException("Whoopsie")
        val viewModel = createViewModel(imageDocument)
        val events = collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        awaitEvent<AnalysisEvent.NotifyError>(events)
    }

    @Test
    @Throws(Exception::class)
    fun should_showPdfInfo_forPdfDocument_afterDocumentWasLoaded() {
        // Given
        val pdfDocument: PdfDocument = PdfDocumentFake()
        val viewModel = spy(createViewModel(pdfDocument))
        val pdfFilename = "Invoice.pdf"
        doReturn(pdfFilename).whenever(viewModel).getPdfFilename(pdfDocument)
        collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        Truth.assertThat(viewModel.uiState.value.pdfInfoPanelVisible).isTrue()
        Truth.assertThat(viewModel.uiState.value.pdfTitle).isEqualTo(pdfFilename)
    }

    @Test
    @Throws(Exception::class)
    fun should_analyzeDocument_afterDocumentWasLoaded() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val viewModel = spy(createViewModel(imageDocument, null))
        collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        verify(viewModel).analyzeDocument()
    }

    @Test
    @Throws(Exception::class)
    fun should_startScanAnimation_whenAnalyzingDocument() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val viewModel = createViewModel(imageDocument, null)
        collectEvents(viewModel)
        val uiStates = collectUiStates(viewModel)

        // When
        viewModel.start()

        // Then
        Truth.assertThat(uiStates.any { it.scanAnimationVisible }).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun should_stopScanAnimation_whenAnalysisFinished() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.complete(
            AnalysisInteractor.ResultHolder(
                AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS,
                "dummy_doc_id",
                "dummy_doc_filename",
            )
        )
        val viewModel =
            createViewModelWithAnalysisFuture(imageDocument, analysisFuture = analysisFuture)
        collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        Truth.assertThat(viewModel.uiState.value.scanAnimationVisible).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun should_requestProceedingToNoExtractionsScreen_whenAnalysisSucceeded_withoutExtractions() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.complete(
            AnalysisInteractor.ResultHolder(
                AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS,
                "dummy_doc_id",
                "dummy_doc_filename",
            )
        )
        val viewModel =
            createViewModelWithAnalysisFuture(imageDocument, analysisFuture = analysisFuture)
        val events = collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        awaitEvent<AnalysisEvent.NotifyNoExtractions>(events)
    }

    @Test
    @Throws(Exception::class)
    fun should_returnExtractions_whenAnalysisSucceeded_withExtractions() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val extractions = Collections.singletonMap(
            "extraction", mock<GiniCaptureSpecificExtraction>()
        )
        val compoundExtractions = Collections.singletonMap(
            "compoundExtraction", mock<GiniCaptureCompoundExtraction>()
        )
        val returnReasons = listOf(
            mock<GiniCaptureReturnReason>()
        )
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.complete(
            AnalysisInteractor.ResultHolder(
                AnalysisInteractor.Result.SUCCESS_WITH_EXTRACTIONS,
                extractions,
                compoundExtractions,
                returnReasons,
                "dummy_doc_id",
                "dummy_doc_filename",
            )
        )
        val viewModel =
            createViewModelWithAnalysisFuture(imageDocument, analysisFuture = analysisFuture)
        val events = collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        val event = awaitEvent<AnalysisEvent.NotifyExtractionsAvailable>(events)
        Truth.assertThat(event.extractions).isEqualTo(extractions)
        Truth.assertThat(event.compoundExtractions).isEqualTo(compoundExtractions)
        Truth.assertThat(event.returnReasons).isEqualTo(returnReasons)
    }

    @Test
    @Throws(Exception::class)
    fun should_clearSavedImages_afterAnalysis_whenNetworkService_wasSet() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.complete(
            AnalysisInteractor.ResultHolder(
                AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS,
                "dummy_doc_id",
                "dummy_doc_filename",
            )
        )
        val viewModel = spy(
            createViewModelWithAnalysisFuture(imageDocument, analysisFuture = analysisFuture)
        )
        collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        verify(viewModel).clearSavedImages()
    }

    @Test
    @Throws(Exception::class)
    fun should_showAlertDialog_forOpenWithPdfDocument_ifAppIsDefaultForPdfs() {
        // Given
        val pdfDocument: PdfDocument = spy(PdfDocumentFake())
        doReturn(Document.ImportMethod.OPEN_WITH).whenever(pdfDocument).importMethod
        val viewModel = spy(createViewModel(pdfDocument, null))
        val pdfFilename = "Invoice.pdf"
        doReturn(pdfFilename).whenever(viewModel).getPdfFilename(pdfDocument)
        val events = collectEvents(viewModel)

        // When
        viewModel.start()
        val callbackCaptor = argumentCaptor<ShowAlertCallback>()
        verify(viewModel).showAlertIfOpenWithDocumentAndAppIsDefault(
            any(),
            callbackCaptor.capture()
        )
        val message = "Message"
        val positiveButton = "Positive Button"
        val onClickListener = DialogInterface.OnClickListener { dialog, which -> }
        val negativeButton = "Negative Button"
        callbackCaptor.firstValue.showAlertDialog(
            message, positiveButton,
            onClickListener, negativeButton, null, null
        )

        // Then
        val event = awaitEvent<AnalysisEvent.ShowAlertDialog>(events)
        Truth.assertThat(event.message).isEqualTo(message)
        Truth.assertThat(event.positiveButtonTitle).isEqualTo(positiveButton)
        Truth.assertThat(event.positiveButtonClickListener).isEqualTo(onClickListener)
        Truth.assertThat(event.negativeButtonTitle).isEqualTo(negativeButton)
    }

    @Test
    @Throws(Exception::class)
    fun should_analyzeDocument_whenAlertDialog_wasClosed_forOpenWithPdfDocument_ifAppIsDefaultForPdfs() {
        // Given
        val pdfDocument: PdfDocument = spy(PdfDocumentFake())
        doReturn(Document.ImportMethod.OPEN_WITH).whenever(pdfDocument).importMethod
        val viewModel = spy(createViewModel(pdfDocument, null))
        val pdfFilename = "Invoice.pdf"
        doReturn(pdfFilename).whenever(viewModel).getPdfFilename(pdfDocument)
        doReturn(CompletableFuture.completedFuture<Void>(null))
            .whenever(viewModel)
            .showAlertIfOpenWithDocumentAndAppIsDefault(any(), any())
        collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        verify(viewModel).doAnalyzeDocument()
    }

    @Test
    @Throws(Exception::class)
    fun should_notifyListener_whenAlertDialog_wasCancelled_forOpenWithPdfDocument_ifAppIsDefaultForPdfs() {
        // Given
        val pdfDocument: PdfDocument = spy(PdfDocumentFake())
        doReturn(Document.ImportMethod.OPEN_WITH).whenever(pdfDocument).importMethod
        val viewModel = spy(createViewModel(pdfDocument, null))
        val pdfFilename = "Invoice.pdf"
        doReturn(pdfFilename).whenever(viewModel).getPdfFilename(pdfDocument)
        val future = CompletableFuture<Void>()
        future.completeExceptionally(CancellationException())
        doReturn(future)
            .whenever(viewModel)
            .showAlertIfOpenWithDocumentAndAppIsDefault(any(), any())
        val events = collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        awaitEvent<AnalysisEvent.NotifyPdfAlertDialogCancelled>(events)
    }

    /**
     * Regression test (PP-2278): [AnalysisViewModel.stop] must cancel the coroutine job which
     * manages post-analysis navigation, so no pending navigation can fire on a dead
     * NavController after the fragment is destroyed.
     */
    @Test
    @Throws(Exception::class)
    fun should_cancel_coroutineJob_whenStopped() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val viewModel = createViewModel(imageDocument, null)
        val job = getJobViaReflection(viewModel)
        Truth.assertThat(job.isActive).isTrue()

        // When: user presses Back (fragment destroyed -> stop() is called)
        viewModel.stop()

        // Then: the job must be cancelled so no pending navigation fires on dead NavController
        Truth.assertThat(job.isCancelled).isTrue()
    }

    private fun getJobViaReflection(viewModel: AnalysisViewModel): Job {
        // The view model is subclassed in createViewModel(), so look the field up on the
        // declaring class
        val field = AnalysisViewModel::class.java.getDeclaredField("job")
        field.isAccessible = true
        return field.get(viewModel) as Job
    }

    @Test
    @Throws(Exception::class)
    fun should_stopScanAnimation_whenStopped() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val viewModel = createViewModel(imageDocument, null)

        // When
        viewModel.stop()

        // Then
        Truth.assertThat(viewModel.uiState.value.scanAnimationVisible).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun should_deleteUploadedDocument_ifAnalysisDidntComplete_whenStopped() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisInteractor = mock<AnalysisInteractor>()
        val viewModel = createViewModel(imageDocument, analysisInteractor = analysisInteractor)

        // When
        viewModel.stop()

        // Then
        verify(analysisInteractor).deleteDocument(any())
    }

    @Test
    @Throws(Exception::class)
    fun should_deleteMultiPageUploadedDocuments_forPdfs_ifAnalysisDidntComplete_whenStopped() {
        // Given
        val pdfDocument: PdfDocument = PdfDocumentFake()
        val analysisInteractor = mock<AnalysisInteractor>()
        val viewModel = createViewModel(pdfDocument, analysisInteractor = analysisInteractor)

        // When
        viewModel.stop()

        // Then
        verify(analysisInteractor).deleteMultiPageDocument(any())
    }

    @Test
    @Throws(Exception::class)
    fun should_clearImageMultiPageDocumentMemoryStore_ifAnalysisCompleted_whenStopped() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.complete(
            AnalysisInteractor.ResultHolder(
                AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS,
                "dummy_doc_id",
                "dummy_doc_filename",
            )
        )
        val memoryStore = mock<ImageMultiPageDocumentMemoryStore>()
        val internal = mock<GiniCapture.Internal>()
        whenever(internal.imageMultiPageDocumentMemoryStore).thenReturn(memoryStore)
        val giniCapture = mock<GiniCapture>()
        whenever(giniCapture.internal()).thenReturn(internal)
        val viewModel = createViewModelWithAnalysisFuture(
            imageDocument,
            giniCapture = giniCapture, analysisFuture = analysisFuture
        )
        collectEvents(viewModel)

        // When
        viewModel.start()
        viewModel.stop()

        // Then
        verify(memoryStore).clear()
    }

    @Test
    @Throws(Exception::class)
    fun should_clearParcelableMemoryCache_whenFinished() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val viewModel = spy(createViewModel(imageDocument, null))

        // When
        viewModel.finish()

        // Then
        verify(viewModel).clearParcelableMemoryCache()
    }

    @Test
    @Throws(Exception::class)
    fun should_notWaitForViewLayout_ifStopped_beforeLoadingDocumentDataFinishes() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val viewModel = spy(createViewModel(imageDocument, null))
        doReturn(true).whenever(viewModel).isStopped
        val events = collectEvents(viewModel, autoLayout = false)

        // When
        viewModel.start()

        // Then
        assertNoEvent<AnalysisEvent.WaitForViewLayout>(events)
    }

    @Test
    @Throws(Exception::class)
    fun should_notReturnError_ifStopped_beforeLoadingDocumentDataFinishes() {
        // Given
        val imageDocument = ImageDocumentFake()
        imageDocument.failWithException = RuntimeException()
        val viewModel = spy(createViewModel(imageDocument))
        doReturn(true).whenever(viewModel).isStopped
        val events = collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        assertNoEvent<AnalysisEvent.NotifyError>(events)
    }

    @Test
    @Throws(Exception::class)
    fun should_notShowDocument_ifStopped_beforeDocumentRendererFinishes() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val bitmap = mock<Bitmap>()
        val rotationForDisplay = 90
        val viewModel = spy(
            createViewModel(imageDocument, null, bitmap, rotationForDisplay)
        )
        doReturn(true).whenever(viewModel).isStopped
        collectEvents(viewModel, previewSize = Size(1024, 768))

        // When
        viewModel.start()

        // Then
        Truth.assertThat(viewModel.uiState.value.documentRender).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun should_triggerErrorEvent_forError_fromReviewScreen() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()
        val exception = Exception("Something is not working")
        GiniCapture.getInstance().internal().reviewScreenAnalysisError = exception
        val errorMessage = "Something went wrong"
        val viewModel = createViewModel(
            imageDocument,
            giniCapture = GiniCapture.getInstance(),
            documentAnalysisErrorMessage = errorMessage
        )
        collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        val errorDetails: MutableMap<String, Any> = HashMap()
        errorDetails[AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.MESSAGE] = errorMessage
        errorDetails[AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.ERROR_OBJECT] = exception
        verify(eventTracker)
            .onAnalysisScreenEvent(Event(AnalysisScreenEvent.ERROR, errorDetails))
    }

    @Test
    @Throws(Exception::class)
    fun should_triggerErrorEvent_forAnalysisError() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        val exception = RuntimeException("error message")
        analysisFuture.completeExceptionally(exception)
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()
        val viewModel = createViewModelWithAnalysisFuture(
            imageDocument,
            giniCapture = GiniCapture.getInstance(), analysisFuture = analysisFuture
        )
        collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        val errorDetails: MutableMap<String, Any?> = HashMap()
        errorDetails[AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.MESSAGE] = exception.message
        errorDetails[AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.ERROR_OBJECT] = exception
        verify(eventTracker)
            .onAnalysisScreenEvent(Event(AnalysisScreenEvent.ERROR, errorDetails))
    }

    // Test for isRAOrSkontoIncludedInExtractions
    @Test
    fun `isRAOrSkontoIncludedInExtractions returns true when Skonto or RA is enabled and valid`() {
        val viewModel = createViewModelWithEmptyImageDocument()
        val resultHolder = AnalysisInteractor.ResultHolder(
            AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS,
            emptyMap(),
            emptyMap(),
            emptyList(),
            "dummy",
            "dummy"
        )
        val bankSDKBridge = mock<BankSDKBridge>()
        val bankSDKProperties = mock<BankSDKProperties>()

        whenever(bankSDKBridge.getBankSDKProperties(any())).thenReturn(bankSDKProperties)
        viewModel.bankSDKBridge = bankSDKBridge

        whenever(bankSDKProperties.isSkontoSDKFlagEnabled).thenReturn(true)
        whenever(bankSDKProperties.isSkontoExtractionsValid).thenReturn(true)
        whenever(bankSDKProperties.isReturnAssistantSDKFlagEnabled).thenReturn(false)
        whenever(bankSDKProperties.isReturnAssistantExtractionsValid).thenReturn(false)

        assertTrue(viewModel.isRAOrSkontoIncludedInExtractions(resultHolder))

        whenever(bankSDKProperties.isSkontoSDKFlagEnabled).thenReturn(false)
        whenever(bankSDKProperties.isSkontoExtractionsValid).thenReturn(false)
        whenever(bankSDKProperties.isReturnAssistantSDKFlagEnabled).thenReturn(true)
        whenever(bankSDKProperties.isReturnAssistantExtractionsValid).thenReturn(true)

        assertTrue(viewModel.isRAOrSkontoIncludedInExtractions(resultHolder))
    }

    @Test
    fun `isRAOrSkontoIncludedInExtractions returns false when neither Skonto nor RA is enabled and valid`() {
        val viewModel = createViewModelWithEmptyImageDocument()
        val resultHolder = AnalysisInteractor.ResultHolder(
            AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS,
            emptyMap(),
            emptyMap(),
            emptyList(),
            "dummy",
            "dummy"
        )
        val bankSDKBridge = mock<BankSDKBridge>()
        val bankSDKProperties = mock<BankSDKProperties>()

        whenever(bankSDKBridge.getBankSDKProperties(any())).thenReturn(bankSDKProperties)
        viewModel.bankSDKBridge = bankSDKBridge

        whenever(bankSDKProperties.isSkontoSDKFlagEnabled).thenReturn(false)
        whenever(bankSDKProperties.isSkontoExtractionsValid).thenReturn(false)
        whenever(bankSDKProperties.isReturnAssistantSDKFlagEnabled).thenReturn(false)
        whenever(bankSDKProperties.isReturnAssistantExtractionsValid).thenReturn(false)

        assertFalse(viewModel.isRAOrSkontoIncludedInExtractions(resultHolder))
    }

    @Test
    fun `isRAOrSkontoIncludedInExtractions returns false when bankSDKBridge is null`() {
        val viewModel = createViewModelWithEmptyImageDocument()
        val resultHolder = AnalysisInteractor.ResultHolder(
            AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS,
            emptyMap(),
            emptyMap(),
            emptyList(),
            "dummy",
            "dummy"
        )

        viewModel.bankSDKBridge = null

        assertFalse(viewModel.isRAOrSkontoIncludedInExtractions(resultHolder))
    }

    @Test
    fun `proceedWithExtractions emits NotifyExtractionsAvailable with correct arguments`() {
        // Arrange
        val viewModel = createViewModelWithEmptyImageDocument()
        val events = collectEvents(viewModel)

        val extractions = mapOf("key1" to mock<GiniCaptureSpecificExtraction>())
        val compoundExtractions = mapOf("key2" to mock<GiniCaptureCompoundExtraction>())
        val returnReasons = listOf(mock<GiniCaptureReturnReason>())

        val resultHolder = mock<AnalysisInteractor.ResultHolder> {
            on { this.extractions } doReturn extractions
            on { this.compoundExtractions } doReturn compoundExtractions
            on { this.returnReasons } doReturn returnReasons
        }

        // Act
        viewModel.proceedWithExtractions(resultHolder)

        // Assert
        val event = awaitEvent<AnalysisEvent.NotifyExtractionsAvailable>(events)
        Truth.assertThat(event.extractions).isEqualTo(extractions)
        Truth.assertThat(event.compoundExtractions).isEqualTo(compoundExtractions)
        Truth.assertThat(event.returnReasons).isEqualTo(returnReasons)
    }

    // region CX extractions — no-results routing

    @Test
    @Throws(Exception::class)
    fun `CX mode - crossBorderPayment absent - should proceed to no-extractions screen`() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.complete(
            AnalysisInteractor.ResultHolder(
                AnalysisInteractor.Result.SUCCESS_WITH_EXTRACTIONS,
                emptyMap(),
                emptyMap(),
                emptyList(),
                "dummy_doc_id",
                "dummy_doc_filename",
            )
        )
        val giniCapture = createGiniCaptureInstanceWithProductTag(ProductTag.CxExtractions)
        val viewModel = createViewModelWithAnalysisFuture(
            imageDocument,
            giniCapture = giniCapture,
            analysisFuture = analysisFuture
        )
        val events = collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        awaitEvent<AnalysisEvent.NotifyNoExtractions>(events)
    }

    @Test
    @Throws(Exception::class)
    fun `CX mode - crossBorderPayment present but empty specificExtractionMaps - should proceed to no-extractions screen`() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val emptyCbp = GiniCaptureCompoundExtraction(CROSS_BORDER_PAYMENT_KEY, emptyList())
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.complete(
            AnalysisInteractor.ResultHolder(
                AnalysisInteractor.Result.SUCCESS_WITH_EXTRACTIONS,
                emptyMap(),
                mapOf(CROSS_BORDER_PAYMENT_KEY to emptyCbp),
                emptyList(),
                "dummy_doc_id",
                "dummy_doc_filename",
            )
        )
        val giniCapture = createGiniCaptureInstanceWithProductTag(ProductTag.CxExtractions)
        val viewModel = createViewModelWithAnalysisFuture(
            imageDocument,
            giniCapture = giniCapture,
            analysisFuture = analysisFuture
        )
        val events = collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        awaitEvent<AnalysisEvent.NotifyNoExtractions>(events)
    }

    @Test
    @Throws(Exception::class)
    fun `CX mode - crossBorderPayment has fields - should forward extractions`() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val cbpRow = mapOf("amount" to mock<GiniCaptureSpecificExtraction>())
        val cbp = GiniCaptureCompoundExtraction(CROSS_BORDER_PAYMENT_KEY, listOf(cbpRow))
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.complete(
            AnalysisInteractor.ResultHolder(
                AnalysisInteractor.Result.SUCCESS_WITH_EXTRACTIONS,
                emptyMap(),
                mapOf(CROSS_BORDER_PAYMENT_KEY to cbp),
                emptyList(),
                "dummy_doc_id",
                "dummy_doc_filename",
            )
        )
        val giniCapture = createGiniCaptureInstanceWithProductTag(ProductTag.CxExtractions)
        val viewModel = createViewModelWithAnalysisFuture(
            imageDocument,
            giniCapture = giniCapture,
            analysisFuture = analysisFuture
        )
        val events = collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        awaitEvent<AnalysisEvent.NotifyExtractionsAvailable>(events)
        assertNoEvent<AnalysisEvent.NotifyNoExtractions>(events)
    }

    @Test
    @Throws(Exception::class)
    fun `SEPA mode - empty specific extractions - should proceed to no-extractions screen (regression)`() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.complete(
            AnalysisInteractor.ResultHolder(
                AnalysisInteractor.Result.SUCCESS_WITH_EXTRACTIONS,
                emptyMap(),
                emptyMap(),
                emptyList(),
                "dummy_doc_id",
                "dummy_doc_filename",
            )
        )
        // Default SEPA product tag
        val viewModel =
            createViewModelWithAnalysisFuture(imageDocument, analysisFuture = analysisFuture)
        val events = collectEvents(viewModel)

        // When
        viewModel.start()

        // Then
        awaitEvent<AnalysisEvent.NotifyNoExtractions>(events)
    }

    // endregion
}
