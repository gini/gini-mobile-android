package net.gini.android.capture.analysis

import android.app.Activity
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
import kotlinx.coroutines.Job
import net.gini.android.capture.AsyncCallback
import net.gini.android.capture.BankSDKBridge
import net.gini.android.capture.BankSDKProperties
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.ProductTag
import net.gini.android.capture.analysis.AnalysisViewModel.Companion.CROSS_BORDER_PAYMENT_KEY
import net.gini.android.capture.di.CaptureSdkIsolatedKoinContext
import net.gini.android.capture.document.DocumentFactory
import net.gini.android.capture.document.GiniCaptureDocument
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.document.ImageDocumentFake
import net.gini.android.capture.document.PdfDocument
import net.gini.android.capture.document.PdfDocumentFake
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
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Shadows.shadowOf
import java.util.Collections
import java.util.concurrent.CancellationException

/**
 * Created by Alpar Szotyori on 10.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 *
 * Ported from the former `AnalysisScreenPresenterTest` after the MVP to MVVM migration of the
 * Analysis screen. Every test case preserves the intent of the original presenter test; view
 * method invocations are asserted through the [AnalysisViewModel.events] and
 * [AnalysisViewModel.scanAnimationActive] LiveData instead of a mocked contract view.
 */
@RunWith(AndroidJUnit4::class)
class AnalysisViewModelTest {

    @Mock
    private lateinit var mActivity: Activity

    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    // GiniBankConfigurationProvider is registered by the Bank SDK at runtime; provide a real
    // instance here so that the payment hint use cases can be resolved through Koin (see
    // MultipageReviewFragmentTest for the same pattern).
    private val koinTestModule = module {
        single { GiniBankConfigurationProvider() }
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        whenever(mActivity.application).thenReturn(app)
        // The analysis result handling depends on providers which require an initialized
        // UserAnalytics instance (as in production)
        UserAnalytics.initialize(InstrumentationRegistry.getInstrumentation().context)
        CaptureSdkIsolatedKoinContext.koin.loadModules(listOf(koinTestModule))
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
        CaptureSdkIsolatedKoinContext.koin.unloadModules(listOf(koinTestModule))
    }

    /**
     * Records all events emitted by the view model and — like the fragment does — responds to
     * [AnalysisViewEvent.WaitForViewLayout] by notifying the view model that the layout finished.
     */
    private class EventRecorder(
        private val pdfPreviewSize: Size = Size(0, 0)
    ) : androidx.lifecycle.Observer<ConsumableEvent<Unit>> {

        lateinit var viewModel: AnalysisViewModel
        lateinit var activity: Activity

        val events = mutableListOf<AnalysisViewEvent>()

        override fun onChanged(value: ConsumableEvent<Unit>) {
            if (value.getContentIfNotHandled() == null) {
                return
            }
            while (true) {
                val event = viewModel.pollEvent() ?: return
                events.add(event)
                if (event is AnalysisViewEvent.WaitForViewLayout) {
                    viewModel.onViewLayoutFinished(pdfPreviewSize, activity)
                }
            }
        }

        inline fun <reified T : AnalysisViewEvent> firstOrNull(): T? =
            events.filterIsInstance<T>().firstOrNull()
    }

    private fun observe(
        viewModel: AnalysisViewModel,
        pdfPreviewSize: Size = Size(0, 0)
    ): EventRecorder {
        val recorder = EventRecorder(pdfPreviewSize)
        recorder.viewModel = viewModel
        recorder.activity = mActivity
        viewModel.events.observeForever(recorder)
        return recorder
    }

    /**
     * Waits for an event matching [predicate] while processing tasks posted to the main looper
     * (needed because post-analysis navigation is dispatched through a background coroutine and
     * `Dispatchers.Main`).
     */
    private fun awaitEvent(
        recorder: EventRecorder,
        timeoutMs: Long = 3000,
        predicate: (AnalysisViewEvent) -> Boolean
    ): AnalysisViewEvent? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            recorder.events.firstOrNull(predicate)?.let { return it }
            Thread.sleep(10)
        }
        return null
    }

    private fun createViewModel(
        document: Document,
        giniCapture: GiniCapture? = createGiniCaptureInstance(),
        bitmap: Bitmap? = null,
        rotationForDisplay: Int = 0,
        pdfPageCount: Int = 0,
        pdfPageCountError: Exception? = null,
        documentAnalysisErrorMessage: String? = null,
        analysisInteractor: AnalysisInteractor? = null
    ): AnalysisViewModel {
        if (giniCapture != null) {
            GiniCaptureHelper.setGiniCaptureInstance(giniCapture)
        }
        val fakeDocumentRenderer = object : DocumentRenderer {
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
                if (pdfPageCountError == null) {
                    asyncCallback.onSuccess(pdfPageCount)
                } else {
                    asyncCallback.onError(pdfPageCountError)
                }
            }
        }
        // By default the analysis never finishes (same as in production while the request is
        // in flight)
        val interactor = analysisInteractor ?: mock {
            on { analyzeMultiPageDocument(any()) } doReturn CompletableFuture()
        }
        val viewModel = object : AnalysisViewModel(
            app, document, documentAnalysisErrorMessage, interactor, false
        ) {
            public override fun createDocumentRenderer() {
                documentRenderer = fakeDocumentRenderer
            }
        }

        val bankSDKBridge = mock<BankSDKBridge>()
        viewModel.setBankSDKBridge(bankSDKBridge)
        return viewModel
    }

    private fun createGiniCaptureInstance(): GiniCapture {
        GiniCapture.newInstance(InstrumentationRegistry.getInstrumentation().context)
            .setGiniCaptureNetworkService(mock())
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
        viewModel.onStart()

        // Then
        verify(viewModel).clearParcelableMemoryCache()
    }

    @Test
    @Throws(Exception::class)
    fun should_startScanAnimation_whenStarted() {
        // Given
        val viewModel = createViewModelWithEmptyImageDocument()

        // When
        viewModel.onStart()

        // Then
        Truth.assertThat(viewModel.scanAnimationActive.value).isTrue()
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
        viewModel.onStart()

        // Then
        verify(document).loadData(eq(app), any())
    }

    @Test
    @Throws(Exception::class)
    fun should_showHints_forImageDocument() {
        // Given
        val viewModel = createViewModelWithEmptyImageDocument()
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        val showHints = recorder.firstOrNull<AnalysisViewEvent.ShowHints>()
        Truth.assertThat(showHints).isNotNull()
        Truth.assertThat(showHints!!.hints).isEqualTo(viewModel.hints)
    }

    @Test
    @Throws(Exception::class)
    fun should_notShowHints_forNonImageDocument() {
        // Given
        val pdfDocument = mock<PdfDocument>()
        whenever(pdfDocument.type).thenReturn(Document.Type.PDF)
        val viewModel = createViewModel(pdfDocument, null)
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        Truth.assertThat(recorder.firstOrNull<AnalysisViewEvent.ShowHints>()).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun should_returnError_throughAnalysisFragmentListener_whenDocumentLoadingFailed() {
        // Given
        val imageDocument = ImageDocumentFake()
        imageDocument.failWithException = RuntimeException("Whoopsie")
        val viewModel = createViewModel(imageDocument)
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        Truth.assertThat(recorder.firstOrNull<AnalysisViewEvent.NotifyError>()).isNotNull()
    }

    @Test
    @Throws(Exception::class)
    fun should_showPdfInfo_forPdfDocument_afterDocumentWasLoaded() {
        // Given
        val pdfDocument: PdfDocument = PdfDocumentFake()
        val pdfPageCount = 3
        val viewModel = spy(
            createViewModel(pdfDocument, pdfPageCount = pdfPageCount)
        )
        val pdfFilename = "Invoice.pdf"
        doReturn(pdfFilename).whenever(viewModel).getPdfFilename(pdfDocument)
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        val showPdfTitle = recorder.firstOrNull<AnalysisViewEvent.ShowPdfTitle>()
        Truth.assertThat(showPdfTitle).isNotNull()
        Truth.assertThat(showPdfTitle!!.title).isEqualTo(pdfFilename)
    }

    @Test
    @Throws(Exception::class)
    fun should_showPdfInfo_withoutPageCount_whenNotAvailable_afterDocumentWasLoaded() {
        // Given
        val pdfDocument: PdfDocument = PdfDocumentFake()
        val viewModel = spy(
            createViewModel(pdfDocument, pdfPageCount = 0)
        )
        val pdfFilename = "Invoice.pdf"
        doReturn(pdfFilename).whenever(viewModel).getPdfFilename(pdfDocument)
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        val showPdfTitle = recorder.firstOrNull<AnalysisViewEvent.ShowPdfTitle>()
        Truth.assertThat(showPdfTitle).isNotNull()
        Truth.assertThat(showPdfTitle!!.title).isEqualTo(pdfFilename)
    }

    @Test
    @Throws(Exception::class)
    fun should_showPdfInfo_withoutPageCount_whenErrorGettingIt_afterDocumentWasLoaded() {
        // Given
        val pdfDocument: PdfDocument = PdfDocumentFake()
        val viewModel = spy(
            createViewModel(pdfDocument, pdfPageCount = 0, pdfPageCountError = RuntimeException())
        )
        val pdfFilename = "Invoice.pdf"
        doReturn(pdfFilename).whenever(viewModel).getPdfFilename(pdfDocument)
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        val showPdfTitle = recorder.firstOrNull<AnalysisViewEvent.ShowPdfTitle>()
        Truth.assertThat(showPdfTitle).isNotNull()
        Truth.assertThat(showPdfTitle!!.title).isEqualTo(pdfFilename)
    }

    @Test
    @Throws(Exception::class)
    fun should_analyzeDocument_afterDocumentWasLoaded() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val viewModel = spy(createViewModel(imageDocument, null))
        val recorder = observe(viewModel)
        recorder.viewModel = viewModel

        // When
        viewModel.onStart()

        // Then
        verify(viewModel).analyzeDocument(any())
    }

    @Test
    @Throws(Exception::class)
    fun should_startScanAnimation_whenAnalyzingDocument() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val viewModel = createViewModel(imageDocument, null)
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        // The scan animation is started when the view model starts and stays active while
        // the document is being analyzed
        Truth.assertThat(recorder.firstOrNull<AnalysisViewEvent.WaitForViewLayout>()).isNotNull()
        Truth.assertThat(viewModel.scanAnimationActive.value).isTrue()
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
        observe(viewModel).also { it.viewModel = viewModel }

        // When
        viewModel.onStart()

        // Then
        Truth.assertThat(viewModel.scanAnimationActive.value).isFalse()
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
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        val event = awaitEvent(recorder) {
            it is AnalysisViewEvent.NotifyProceedToNoExtractionsScreen
        }
        Truth.assertThat(event).isNotNull()
    }

    @Test
    @Throws(Exception::class)
    fun should_returnExtractions_whenAnalysisSucceeded_withExtractions() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val extractions = Collections.singletonMap(
            "extraction", mock<GiniCaptureSpecificExtraction>()
        )
        val compoundExtraction = Collections.singletonMap(
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
                compoundExtraction,
                returnReasons,
                "dummy_doc_id",
                "dummy_doc_filename",
            )
        )
        val viewModel =
            createViewModelWithAnalysisFuture(imageDocument, analysisFuture = analysisFuture)
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        val event = awaitEvent(recorder) {
            it is AnalysisViewEvent.NotifyExtractionsAvailable
        } as AnalysisViewEvent.NotifyExtractionsAvailable?
        Truth.assertThat(event).isNotNull()
        Truth.assertThat(event!!.extractions).isEqualTo(extractions)
        Truth.assertThat(event.compoundExtractions).isEqualTo(compoundExtraction)
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
        val recorder = observe(viewModel)
        recorder.viewModel = viewModel

        // When
        viewModel.onStart()

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
        val recorder = observe(viewModel)
        recorder.viewModel = viewModel

        // When
        viewModel.onStart()
        val callbackCaptor = argumentCaptor<ShowAlertCallback>()
        verify(viewModel).showAlertIfOpenWithDocumentAndAppIsDefault(
            any(),
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
        val showAlertDialog = recorder.firstOrNull<AnalysisViewEvent.ShowAlertDialog>()
        Truth.assertThat(showAlertDialog).isNotNull()
        Truth.assertThat(showAlertDialog!!.message).isEqualTo(message)
        Truth.assertThat(showAlertDialog.positiveButtonTitle).isEqualTo(positiveButton)
        Truth.assertThat(showAlertDialog.positiveButtonClickListener).isEqualTo(onClickListener)
        Truth.assertThat(showAlertDialog.negativeButtonTitle).isEqualTo(negativeButton)
        Truth.assertThat(showAlertDialog.negativeButtonClickListener).isNull()
        Truth.assertThat(showAlertDialog.cancelListener).isNull()
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
            .showAlertIfOpenWithDocumentAndAppIsDefault(any(), any(), any())
        val recorder = observe(viewModel)
        recorder.viewModel = viewModel

        // When
        viewModel.onStart()

        // Then
        verify(viewModel).doAnalyzeDocument()
    }

    @Test
    @Throws(Exception::class)
    fun should_notifiyListener_whenAlertDialog_wasCancelled_forOpenWithPdfDocument_ifAppIsDefaultForPdfs() {
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
            .showAlertIfOpenWithDocumentAndAppIsDefault(any(), any(), any())
        val recorder = observe(viewModel)
        recorder.viewModel = viewModel

        // When
        viewModel.onStart()

        // Then
        Truth.assertThat(
            recorder.firstOrNull<AnalysisViewEvent.NotifyDefaultPDFAppAlertDialogCancelled>()
        ).isNotNull()
    }

    // ── PP-2278 regression test (Fix 3) ───────────────────────────────────────

    /**
     * [AnalysisViewModel.onStop] must cancel the coroutine job so that all coroutines managing
     * post-analysis navigation are cancelled when the fragment is destroyed.
     *
     * Without this, the coroutine scope is never cancelled and a pending navigation can still
     * fire on a dead NavController, causing an NPE crash.
     *
     * **Fails when reverted**: Removing `job.cancel()` from [AnalysisViewModel.onStop]
     * leaves the job active after onStop() and this assertion fails.
     */
    @Test
    @Throws(Exception::class)
    fun should_cancel_coroutineScope_whenStopped() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val viewModel = createViewModel(imageDocument, null)
        val jobField = AnalysisViewModel::class.java.getDeclaredField("job")
        jobField.isAccessible = true
        val job = jobField.get(viewModel) as Job
        Truth.assertThat(job.isActive).isTrue()

        // When: user presses Back (fragment destroyed -> onStop() is called)
        viewModel.onStop()

        // Then: the scope must be cancelled so no pending navigation fires on dead NavController
        Truth.assertThat(job.isCancelled).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun should_stopScanAnimation_whenStopped() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val viewModel = createViewModel(imageDocument, null)
        observe(viewModel)
        viewModel.onStart()
        Truth.assertThat(viewModel.scanAnimationActive.value).isTrue()

        // When
        viewModel.onStop()

        // Then
        Truth.assertThat(viewModel.scanAnimationActive.value).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun should_deleteUploadedDocument_ifAnalysisDidntComplete_whenStopped() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisInteractor = mock<AnalysisInteractor>()
        val viewModel = createViewModel(imageDocument, analysisInteractor = analysisInteractor)

        // When
        viewModel.onStop()

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
        viewModel.onStop()

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
        observe(viewModel)

        // When
        viewModel.onStart()
        viewModel.onStop()

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
        doReturn(true).whenever(viewModel).isStopped()
        val recorder = observe(viewModel)
        recorder.viewModel = viewModel

        // When
        viewModel.onStart()

        // Then
        Truth.assertThat(recorder.firstOrNull<AnalysisViewEvent.WaitForViewLayout>()).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun should_notReturnError_ifStopped_beforeLoadingDocumentDataFinishes() {
        // Given
        val imageDocument = ImageDocumentFake()
        imageDocument.failWithException = RuntimeException()
        val viewModel = spy(createViewModel(imageDocument))
        doReturn(true).whenever(viewModel).isStopped()
        val recorder = observe(viewModel)
        recorder.viewModel = viewModel

        // When
        viewModel.onStart()

        // Then
        Truth.assertThat(recorder.firstOrNull<AnalysisViewEvent.NotifyError>()).isNull()
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
        doReturn(true).whenever(viewModel).isStopped()
        val recorder = observe(viewModel, pdfPreviewSize = Size(1024, 768))
        recorder.viewModel = viewModel

        // When
        viewModel.onStart()

        // Then
        Truth.assertThat(recorder.firstOrNull<AnalysisViewEvent.ShowBitmap>()).isNull()
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
        observe(viewModel)

        // When
        viewModel.onStart()

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
        observe(viewModel)

        // When
        viewModel.onStart()

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
        val viewModel = createViewModel(ImageDocumentFake(), null)
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
        viewModel.setBankSDKBridge(bankSDKBridge)

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
        val viewModel = createViewModel(ImageDocumentFake(), null)
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
        viewModel.setBankSDKBridge(bankSDKBridge)

        whenever(bankSDKProperties.isSkontoSDKFlagEnabled).thenReturn(false)
        whenever(bankSDKProperties.isSkontoExtractionsValid).thenReturn(false)
        whenever(bankSDKProperties.isReturnAssistantSDKFlagEnabled).thenReturn(false)
        whenever(bankSDKProperties.isReturnAssistantExtractionsValid).thenReturn(false)

        assertFalse(viewModel.isRAOrSkontoIncludedInExtractions(resultHolder))
    }

    @Test
    fun `isRAOrSkontoIncludedInExtractions returns false when bankSDKBridge is null`() {
        val viewModel = createViewModel(ImageDocumentFake(), null)
        val resultHolder = AnalysisInteractor.ResultHolder(
            AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS,
            emptyMap(),
            emptyMap(),
            emptyList(),
            "dummy",
            "dummy"
        )

        viewModel.setBankSDKBridge(null)

        assertFalse(viewModel.isRAOrSkontoIncludedInExtractions(resultHolder))
    }

    @Test
    fun `proceedWithExtractionsWhenEducationFinished emits extractions after education finished`() {
        // Given
        val viewModel = createViewModel(ImageDocumentFake(), null)
        val resultHolder = AnalysisInteractor.ResultHolder(
            AnalysisInteractor.Result.SUCCESS_WITH_EXTRACTIONS,
            mapOf("key1" to mock<GiniCaptureSpecificExtraction>()),
            emptyMap(),
            emptyList(),
            "dummy",
            "dummy"
        )
        val recorder = observe(viewModel)

        // When
        viewModel.proceedWithExtractionsWhenEducationFinished(
            resultHolder,
            isSavingInvoicesInProgress = false
        )

        // Then
        val event = awaitEvent(recorder) { it is AnalysisViewEvent.NotifyExtractionsAvailable }
        Truth.assertThat(event).isNotNull()
    }

    @Test
    fun `proceedWithExtractions emits extractions available event with correct arguments`() {
        // Arrange
        val viewModel = createViewModel(ImageDocumentFake(), null)
        val recorder = observe(viewModel)

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
        val event = recorder.firstOrNull<AnalysisViewEvent.NotifyExtractionsAvailable>()
        Truth.assertThat(event).isNotNull()
        Truth.assertThat(event!!.extractions).isEqualTo(extractions)
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
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        val event = awaitEvent(recorder) {
            it is AnalysisViewEvent.NotifyProceedToNoExtractionsScreen
        }
        Truth.assertThat(event).isNotNull()
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
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        val event = awaitEvent(recorder) {
            it is AnalysisViewEvent.NotifyProceedToNoExtractionsScreen
        }
        Truth.assertThat(event).isNotNull()
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
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        val event = awaitEvent(recorder) { it is AnalysisViewEvent.NotifyExtractionsAvailable }
        Truth.assertThat(event).isNotNull()
        Truth.assertThat(
            recorder.firstOrNull<AnalysisViewEvent.NotifyProceedToNoExtractionsScreen>()
        ).isNull()
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
        val recorder = observe(viewModel)

        // When
        viewModel.onStart()

        // Then
        val event = awaitEvent(recorder) {
            it is AnalysisViewEvent.NotifyProceedToNoExtractionsScreen
        }
        Truth.assertThat(event).isNotNull()
    }

    // endregion

    private fun createGiniCaptureInstanceWithProductTag(productTag: ProductTag): GiniCapture {
        GiniCapture.newInstance(InstrumentationRegistry.getInstrumentation().context)
            .setGiniCaptureNetworkService(mock())
            .setProductTag(productTag)
            .build()
        return GiniCapture.getInstance()
    }
}
