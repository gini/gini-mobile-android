package net.gini.android.capture.analysis

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Bitmap
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.*
import jersey.repackaged.jsr166e.CompletableFuture
import net.gini.android.capture.*
import net.gini.android.capture.document.*
import net.gini.android.capture.internal.document.DocumentRenderer
import net.gini.android.capture.internal.document.ImageMultiPageDocumentMemoryStore
import net.gini.android.capture.internal.ui.ErrorSnackbar
import net.gini.android.capture.internal.util.FileImportHelper.ShowAlertCallback
import net.gini.android.capture.internal.util.Size
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.*
import java.util.concurrent.CancellationException

/**
 * Created by Alpar Szotyori on 10.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
@RunWith(AndroidJUnit4::class)
class AnalysisScreenPresenterTest {
    @Mock
    private lateinit var mActivity: Activity

    @Mock
    private lateinit var mView: AnalysisScreenContract.View
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun should_convertSinglePageDocument_intoMultiPage() {
        // Given
        val document: GiniCaptureDocument = DocumentFactory.newEmptyImageDocument(
            Document.Source.newCameraSource(), Document.ImportMethod.NONE
        )

        // When
        val presenter = createPresenter(document, null)

        // Then
        val documentInMultiPage = presenter.multiPageDocument.documents[0]
        Truth.assertThat(documentInMultiPage).isEqualTo(document)
    }

    private fun createPresenter(
        document: Document,
        giniCapture: GiniCapture? = createGiniCaptureInstance(),
        bitmap: Bitmap? = null,
        rotationForDisplay: Int = 0,
        pdfPageCount: Int = 0,
        pdfPageCountError: Exception? = null,
        documentAnalysisErrorMessage: String? = null,
        analysisInteractor: AnalysisInteractor? = null
    ): AnalysisScreenPresenter {
        if (giniCapture != null) {
            GiniCaptureHelper.setGiniCaptureInstance(giniCapture)
        }
        whenever(mView.waitForViewLayout())
            .thenReturn(CompletableFuture.completedFuture(null))
        whenever(mView.pdfPreviewSize).thenReturn(Size(0, 0))
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
                if (pdfPageCountError == null) {
                    asyncCallback.onSuccess(pdfPageCount)
                } else {
                    asyncCallback.onError(pdfPageCountError)
                }
            }
        }
        val listener = mock<AnalysisFragmentListener>()
        val presenter: AnalysisScreenPresenter
        if (analysisInteractor == null) {
            presenter = object : AnalysisScreenPresenter(
                mActivity, mView,
                document, documentAnalysisErrorMessage
            ) {
                public override fun createDocumentRenderer() {
                    mDocumentRenderer = documentRenderer
                }
            }
        } else {
            presenter = object : AnalysisScreenPresenter(
                mActivity, mView, document,
                documentAnalysisErrorMessage,
                analysisInteractor
            ) {
                public override fun createDocumentRenderer() {
                    mDocumentRenderer = documentRenderer
                }
            }
        }
        presenter.setListener(listener)
        return presenter
    }

    private fun createGiniCaptureInstance(): GiniCapture {
        GiniCapture.cleanup(InstrumentationRegistry.getInstrumentation().targetContext)
        GiniCapture.newInstance()
            .setGiniCaptureNetworkApi(mock())
            .setGiniCaptureNetworkService(mock())
            .build()
        return GiniCapture.getInstance()
    }

    @Test
    @Throws(Exception::class)
    fun should_tagDocuments_forParcelableMemoryCache() {
        // Given
        val document: GiniCaptureDocument = DocumentFactory.newEmptyImageDocument(
            Document.Source.newCameraSource(), Document.ImportMethod.NONE
        )

        // When
        val presenter = createPresenter(document, null)

        // Then
        Truth.assertThat(document.parcelableMemoryCacheTag)
            .isEqualTo(AnalysisScreenPresenter.PARCELABLE_MEMORY_CACHE_TAG)
        Truth.assertThat(presenter.multiPageDocument.parcelableMemoryCacheTag)
            .isEqualTo(AnalysisScreenPresenter.PARCELABLE_MEMORY_CACHE_TAG)
    }

    @Test
    @Throws(Exception::class)
    fun should_generateHintsList_withRandomOrder() {
        // Given
        val presenters: MutableList<AnalysisScreenPresenter> = ArrayList()
        val nrOfPresenters = 5
        for (i in 0 until nrOfPresenters) {
            presenters.add(createPresenterWithEmptyImageDocument())
        }

        // Then
        assertHaveDifferentHintOrders(presenters)
    }

    private fun createPresenterWithEmptyImageDocument(): AnalysisScreenPresenter {
        val document: GiniCaptureDocument = DocumentFactory.newEmptyImageDocument(
            Document.Source.newCameraSource(), Document.ImportMethod.NONE
        )
        document.data = ByteArray(42)
        return createPresenter(document)
    }

    private fun assertHaveDifferentHintOrders(presenters: List<AnalysisScreenPresenter>) {
        val hints1 = presenters[0].hints
        var countSamePosition = 0
        for (i in hints1.indices) {
            for (j in presenters.indices) {
                val lhs = presenters[j]
                for (k in j + 1 until presenters.size) {
                    val rhs = presenters[k]
                    if (lhs.hints[i] == rhs.hints[i]) {
                        countSamePosition++
                    }
                }
            }
        }
        val nrOfComparisons = presenters.size - 1
        val nrOfPairwiseComparisons = (nrOfComparisons / 2.0 * (nrOfComparisons + 1)).toInt()
        val samePositionCountIfSameOrder = nrOfPairwiseComparisons * hints1.size
        Truth.assertThat(countSamePosition).isLessThan(samePositionCountIfSameOrder)
    }

    @Test
    @Throws(Exception::class)
    fun should_hideError() {
        // Given
        val presenter = createPresenterWithEmptyImageDocument()

        // When
        presenter.hideError()

        // Then
        verify(mView).hideErrorSnackbar()
    }

    @Test
    @Throws(Exception::class)
    fun should_showError() {
        // Given
        val presenter = createPresenterWithEmptyImageDocument()

        // When
        val message = "Error message"
        val duration = 1000
        presenter.showError(message, duration)

        // Then
        verify(mView).showErrorSnackbar(message, duration, null, null)
    }

    @Test
    @Throws(Exception::class)
    fun should_showError_withButtonTitle_andOnClickListener() {
        // Given
        val presenter = createPresenterWithEmptyImageDocument()

        // When
        val message = "Error message"
        val buttonTitle = "Retry"
        val onClickListener = View.OnClickListener { }
        presenter.showError(message, buttonTitle, onClickListener)

        // Then
        verify(mView).showErrorSnackbar(
            message, ErrorSnackbar.LENGTH_INDEFINITE, buttonTitle,
            onClickListener
        )
    }

    @Test
    @Throws(Exception::class)
    fun should_clearParcelableMemoryCache_whenStarted() {
        // Given
        val presenter = spy(createPresenterWithEmptyImageDocument())

        // When
        presenter.start()

        // Then
        verify(presenter).clearParcelableMemoryCache()
    }

    @Test
    @Throws(Exception::class)
    fun should_startScanAnimation_whenStarted() {
        // Given
        val presenter = createPresenterWithEmptyImageDocument()

        // When
        presenter.start()

        // Then
        verify(mView, atLeastOnce()).showScanAnimation()
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
        val presenter = createPresenter(document, null)

        // When
        presenter.start()

        // Then
        verify(document).loadData(eq(mActivity), any())
    }

    @Test
    @Throws(Exception::class)
    fun should_showHints_forImageDocument() {
        // Given
        val presenter = createPresenterWithEmptyImageDocument()

        // When
        presenter.start()

        // Then
        verify(mView).showHints(presenter.hints)
    }

    @Test
    @Throws(Exception::class)
    fun should_notShowHints_forNonImageDocument() {
        // Given
        val pdfDocument = mock<PdfDocument>()
        whenever(pdfDocument.type).thenReturn(Document.Type.PDF)
        val presenter = createPresenter(pdfDocument, null)

        // When
        presenter.start()

        // Then
        verify(mView, never()).showHints(presenter.hints)
    }

    @Test
    @Throws(Exception::class)
    fun should_returnError_throughAnalysisFragmentListener_whenDocumentLoadingFailed() {
        // Given
        val imageDocument = ImageDocumentFake()
        imageDocument.failWithException = RuntimeException("Whoopsie")
        val presenter = createPresenter(imageDocument)
        val listener = mock<AnalysisFragmentListener>()
        presenter.setListener(listener)

        // When
        presenter.start()

        // Then
        verify(listener).onError(any())
    }

    @Test
    @Throws(Exception::class)
    fun should_showPdfInfo_forPdfDocument_afterDocumentWasLoaded() {
        // Given
        val pdfDocument: PdfDocument = PdfDocumentFake()
        val pdfPageCount = 3
        val pdfPageCountString = "$pdfPageCount pages"
        val resources = mock<Resources>()
        whenever(
            resources.getQuantityString(anyInt(), anyInt(), any())
        ).thenReturn(pdfPageCountString)
        whenever(mActivity.resources).thenReturn(resources)
        val presenter = spy(
            createPresenter(pdfDocument, pdfPageCount = pdfPageCount)
        )
        val pdfFilename = "Invoice.pdf"
        doReturn(pdfFilename).whenever(presenter).getPdfFilename(pdfDocument)

        // When
        presenter.start()

        // Then
        verify(mView).showPdfInfoPanel()
        verify(mView).showPdfTitle(pdfFilename)
        verify(mView).showPdfPageCount(pdfPageCountString)
    }

    @Test
    @Throws(Exception::class)
    fun should_showPdfInfo_withoutPageCount_whenNotAvailable_afterDocumentWasLoaded() {
        // Given
        val pdfDocument: PdfDocument = PdfDocumentFake()
        val presenter = spy(
            createPresenter(pdfDocument, pdfPageCount = 0)
        )
        val pdfFilename = "Invoice.pdf"
        doReturn(pdfFilename).whenever(presenter).getPdfFilename(pdfDocument)

        // When
        presenter.start()

        // Then
        verify(mView).showPdfInfoPanel()
        verify(mView).showPdfTitle(pdfFilename)
        verify(mView).hidePdfPageCount()
    }

    @Test
    @Throws(Exception::class)
    fun should_showPdfInfo_withoutPageCount_whenErrorGettingIt_afterDocumentWasLoaded() {
        // Given
        val pdfDocument: PdfDocument = PdfDocumentFake()
        val presenter = spy(
            createPresenter(pdfDocument, pdfPageCount = 0, pdfPageCountError = RuntimeException())
        )
        val pdfFilename = "Invoice.pdf"
        doReturn(pdfFilename).whenever(presenter).getPdfFilename(pdfDocument)

        // When
        presenter.start()

        // Then
        verify(mView).showPdfInfoPanel()
        verify(mView).showPdfTitle(pdfFilename)
        verify(mView).hidePdfPageCount()
    }

    @Test
    @Throws(Exception::class)
    fun should_showDocument_afterDocumentWasLoaded() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        whenever(mView.pdfPreviewSize).thenReturn(Size(1024, 768))
        val bitmap = mock<Bitmap>()
        val rotationForDisplay = 90
        val presenter = spy(
            createPresenter(imageDocument, bitmap = bitmap, rotationForDisplay = rotationForDisplay)
        )

        // When
        presenter.start()

        // Then
        verify(mView).showBitmap(bitmap, rotationForDisplay)
    }

    @Test
    @Throws(Exception::class)
    fun should_analyzeDocument_afterDocumentWasLoaded() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val presenter = spy(createPresenter(imageDocument, null))

        // When
        presenter.start()

        // Then
        verify(presenter).analyzeDocument()
    }

    @Test
    @Throws(Exception::class)
    fun should_showError_ifAvailable_beforeAnalysis() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val errorMessage = "Something went wrong"
        val presenter = spy(
            createPresenter(imageDocument, documentAnalysisErrorMessage = errorMessage)
        )

        // When
        presenter.start()

        // Then
        verify(mView).showErrorSnackbar(
            eq(errorMessage),
            anyInt(),
            anyOrNull(),
            any()
        )
    }

    @Test
    @Throws(Exception::class)
    fun should_analyzeDocument_afterTappingRetry_onErrorSnackbar() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val errorMessage = "Something went wrong"
        val presenter = spy(createPresenter(imageDocument, documentAnalysisErrorMessage = errorMessage))

        // When
        presenter.start()
        val onClickListenerCaptor = argumentCaptor<View.OnClickListener>()
        verify(mView).showErrorSnackbar(
            eq(errorMessage),
            anyInt(),
            anyOrNull(),
            onClickListenerCaptor.capture()
        )
        onClickListenerCaptor.firstValue.onClick(mock())

        // Then
        verify(presenter).doAnalyzeDocument()
    }

    @Test
    @Throws(Exception::class)
    fun should_startScanAnimation_whenAnalyzingDocument() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val presenter = createPresenter(imageDocument, null)

        // When
        presenter.start()

        // Then
        // Two times, because scan animation is also started when starting the presenter
        verify(mView, atLeast(2)).showScanAnimation()
    }

    @Test
    @Throws(Exception::class)
    fun should_stopScanAnimation_whenAnalysisFinished() {
        // Given
        whenever(mActivity.getString(anyInt())).thenReturn("A String")
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.complete(
            AnalysisInteractor.ResultHolder(
                AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS
            )
        )
        val presenter = createPresenterWithAnalysisFuture(imageDocument, analysisFuture = analysisFuture)

        // When
        presenter.start()

        // Then
        verify(mView).hideScanAnimation()
    }

    private fun createPresenterWithAnalysisFuture(
        document: Document,
        giniCapture: GiniCapture? = createGiniCaptureInstance(),
        analysisFuture: CompletableFuture<AnalysisInteractor.ResultHolder>
    ): AnalysisScreenPresenter {
        val analysisInteractor = mock<AnalysisInteractor> {
            on { analyzeMultiPageDocument(any()) } doReturn analysisFuture
        }
        return createPresenter(document, giniCapture = giniCapture, analysisInteractor = analysisInteractor)
    }

    @Test
    @Throws(Exception::class)
    fun should_showError_whenAnalysisFailed() {
        // Given
        whenever(mActivity.getString(anyInt())).thenReturn("A String")
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.completeExceptionally(RuntimeException())
        val presenter = createPresenterWithAnalysisFuture(imageDocument, analysisFuture = analysisFuture)

        // When
        presenter.start()

        // Then
        verify(mView).showErrorSnackbar(
            any(), anyInt(), any(), any()
        )
    }

    @Test
    @Throws(Exception::class)
    fun should_requestProceedingToNoExtractionsScreen_whenAnalysisSucceeded_withoutExtractions() {
        // Given
        whenever(mActivity.getString(anyInt())).thenReturn("A String")
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.complete(
            AnalysisInteractor.ResultHolder(
                AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS
            )
        )
        val presenter = createPresenterWithAnalysisFuture(imageDocument, analysisFuture = analysisFuture)
        val listener = mock<AnalysisFragmentListener>()
        presenter.setListener(listener)

        // When
        presenter.start()

        // Then
        verify(listener).onProceedToNoExtractionsScreen(any())
    }

    @Test
    @Throws(Exception::class)
    fun should_returnExtractions_whenAnalysisSucceeded_withExtractions() {
        // Given
        whenever(mActivity.getString(anyInt())).thenReturn("A String")
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
                extractions, compoundExtraction, returnReasons
            )
        )
        val presenter = createPresenterWithAnalysisFuture(imageDocument, analysisFuture = analysisFuture)
        val listener = mock<AnalysisFragmentListener>()
        presenter.setListener(listener)

        // When
        presenter.start()

        // Then
        verify(listener)
            .onExtractionsAvailable(extractions, compoundExtraction, returnReasons)
    }

    @Test
    @Throws(Exception::class)
    fun should_clearSavedImages_afterAnalysis_whenNetworkService_wasSet() {
        // Given
        whenever(mActivity.getString(anyInt())).thenReturn("A String")
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.complete(
            AnalysisInteractor.ResultHolder(
                AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS
            )
        )
        val presenter = spy(
            createPresenterWithAnalysisFuture(imageDocument, analysisFuture = analysisFuture)
        )
        val listener = mock<AnalysisFragmentListener>()
        presenter.setListener(listener)

        // When
        presenter.start()

        // Then
        verify(presenter).clearSavedImages()
    }

    @Test
    @Throws(Exception::class)
    fun should_showAlertDialog_forOpenWithPdfDocument_ifAppIsDefaultForPdfs() {
        // Given
        val pdfDocument: PdfDocument = spy(PdfDocumentFake())
        doReturn(Document.ImportMethod.OPEN_WITH).whenever(pdfDocument).importMethod
        val presenter = spy(createPresenter(pdfDocument, null))
        val pdfFilename = "Invoice.pdf"
        doReturn(pdfFilename).whenever(presenter).getPdfFilename(pdfDocument)

        // When
        presenter.start()
        val callbackCaptor = argumentCaptor<ShowAlertCallback>()
        verify(presenter).showAlertIfOpenWithDocumentAndAppIsDefault(
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
        verify(mView).showAlertDialog(
            message, positiveButton,
            onClickListener, negativeButton, null, null
        )
    }

    @Test
    @Throws(Exception::class)
    fun should_analyzeDocument_whenAlertDialog_wasClosed_forOpenWithPdfDocument_ifAppIsDefaultForPdfs() {
        // Given
        val pdfDocument: PdfDocument = spy(PdfDocumentFake())
        doReturn(Document.ImportMethod.OPEN_WITH).whenever(pdfDocument).importMethod
        val presenter = spy(createPresenter(pdfDocument, null))
        val pdfFilename = "Invoice.pdf"
        doReturn(pdfFilename).whenever(presenter).getPdfFilename(pdfDocument)
        doReturn(CompletableFuture.completedFuture<Any?>(null))
            .whenever(presenter)
            .showAlertIfOpenWithDocumentAndAppIsDefault(any(), any())

        // When
        presenter.start()

        // Then
        verify(presenter).doAnalyzeDocument()
    }

    @Test
    @Throws(Exception::class)
    fun should_notifiyListener_whenAlertDialog_wasCancelled_forOpenWithPdfDocument_ifAppIsDefaultForPdfs() {
        // Given
        val pdfDocument: PdfDocument = spy(PdfDocumentFake())
        doReturn(Document.ImportMethod.OPEN_WITH).whenever(pdfDocument).importMethod
        val presenter = spy(createPresenter(pdfDocument, null))
        val pdfFilename = "Invoice.pdf"
        doReturn(pdfFilename).whenever(presenter).getPdfFilename(pdfDocument)
        val future = CompletableFuture<Void>()
        future.completeExceptionally(CancellationException())
        doReturn(future)
            .whenever(presenter)
            .showAlertIfOpenWithDocumentAndAppIsDefault(any(), any()
            )
        val listener = mock<AnalysisFragmentListener>()
        presenter.setListener(listener)

        // When
        presenter.start()

        // Then
        verify(listener).onDefaultPDFAppAlertDialogCancelled()
    }

    @Test
    @Throws(Exception::class)
    fun should_stopScanAnimation_whenStopped() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val presenter = createPresenter(imageDocument, null)

        // When
        presenter.stop()

        // Then
        verify(mView).hideScanAnimation()
    }

    @Test
    @Throws(Exception::class)
    fun should_deleteUploadedDocument_ifAnalysisDidntComplete_whenStopped() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisInteractor = mock<AnalysisInteractor>()
        val presenter = createPresenter(imageDocument, analysisInteractor = analysisInteractor)

        // When
        presenter.stop()

        // Then
        verify(analysisInteractor).deleteDocument(any())
    }

    @Test
    @Throws(Exception::class)
    fun should_deleteMultiPageUploadedDocuments_forPdfs_ifAnalysisDidntComplete_whenStopped() {
        // Given
        val pdfDocument: PdfDocument = PdfDocumentFake()
        val analysisInteractor = mock<AnalysisInteractor>()
        val presenter = createPresenter(pdfDocument, analysisInteractor = analysisInteractor)

        // When
        presenter.stop()

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
                AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS
            )
        )
        val memoryStore = mock<ImageMultiPageDocumentMemoryStore>()
        val internal = mock<GiniCapture.Internal>()
        whenever(internal.imageMultiPageDocumentMemoryStore).thenReturn(memoryStore)
        val giniCapture = mock<GiniCapture>()
        whenever(giniCapture.internal()).thenReturn(internal)
        val presenter = createPresenterWithAnalysisFuture(imageDocument,
            giniCapture = giniCapture, analysisFuture = analysisFuture
        )

        // When
        presenter.start()
        presenter.stop()

        // Then
        verify(memoryStore).clear()
    }

    @Test
    @Throws(Exception::class)
    fun should_clearParcelableMemoryCache_whenFinished() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val presenter = spy(createPresenter(imageDocument, null))

        // When
        presenter.finish()

        // Then
        verify(presenter).clearParcelableMemoryCache()
    }

    @Test
    @Throws(Exception::class)
    fun should_notWaitForViewLayout_ifStopped_beforeLoadingDocumentDataFinishes() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val presenter = spy(createPresenter(imageDocument, null))
        doReturn(true).whenever(presenter).isStopped

        // When
        presenter.start()

        // Then
        verify(mView, never()).waitForViewLayout()
    }

    @Test
    @Throws(Exception::class)
    fun should_notReturnError_ifStopped_beforeLoadingDocumentDataFinishes() {
        // Given
        val imageDocument = ImageDocumentFake()
        imageDocument.failWithException = RuntimeException()
        val presenter = spy(createPresenter(imageDocument))
        doReturn(true).whenever(presenter).isStopped
        val listener = mock<AnalysisFragmentListener>()
        presenter.setListener(listener)

        // When
        presenter.start()

        // Then
        verify(listener, never()).onError(any())
    }

    @Test
    @Throws(Exception::class)
    fun should_notShowDocument_ifStopped_beforeDocumentRendererFinishes() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        whenever(mView.pdfPreviewSize).thenReturn(Size(1024, 768))
        val bitmap = mock<Bitmap>()
        val rotationForDisplay = 90
        val presenter = spy(
            createPresenter(imageDocument, null, bitmap, rotationForDisplay)
        )
        doReturn(true).whenever(presenter).isStopped

        // When
        presenter.start()

        // Then
        verify(mView, never()).showBitmap(bitmap, rotationForDisplay)
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
        val presenter = createPresenter(
            imageDocument,
            giniCapture = GiniCapture.getInstance(),
            documentAnalysisErrorMessage = errorMessage
        )

        // When
        presenter.start()

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
        val presenter = createPresenterWithAnalysisFuture(
            imageDocument,
            giniCapture = GiniCapture.getInstance(), analysisFuture = analysisFuture
        )

        // When
        presenter.start()

        // Then
        val errorDetails: MutableMap<String, Any?> = HashMap()
        errorDetails[AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.MESSAGE] = exception.message
        errorDetails[AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.ERROR_OBJECT] = exception
        verify(eventTracker)
            .onAnalysisScreenEvent(Event(AnalysisScreenEvent.ERROR, errorDetails))
    }

    @Test
    @Throws(Exception::class)
    fun should_triggerRetryEvent_forError_fromReviewScreen_whenRetry_wasClicked() {
        // Given
        val imageDocument: ImageDocument = ImageDocumentFake()
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()
        val errorMessage = "Something went wrong"
        val presenter = createPresenter(
            imageDocument,
            giniCapture = GiniCapture.getInstance(),
            documentAnalysisErrorMessage = errorMessage
        )

        // When
        presenter.start()
        val onClickListenerCaptor = argumentCaptor<View.OnClickListener>()
        verify(mView).showErrorSnackbar(any(), anyInt(), anyOrNull(), onClickListenerCaptor.capture())
        onClickListenerCaptor.firstValue.onClick(mock())

        // Then
        verify(eventTracker).onAnalysisScreenEvent(Event(AnalysisScreenEvent.RETRY))
    }

    @Test
    @Throws(Exception::class)
    fun should_triggerRetryEvent_forAnalysisError_whenRetry_wasClicked() {
        // Given
        whenever(mActivity.getString(anyInt())).thenReturn("A String")
        val imageDocument: ImageDocument = ImageDocumentFake()
        val analysisFuture = CompletableFuture<AnalysisInteractor.ResultHolder>()
        analysisFuture.completeExceptionally(RuntimeException("error message"))
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()
        val presenter = createPresenterWithAnalysisFuture(
            imageDocument,
            giniCapture = GiniCapture.getInstance(), analysisFuture = analysisFuture
        )

        // When
        presenter.start()
        val onClickListenerCaptor = argumentCaptor<View.OnClickListener>()
        verify(mView).showErrorSnackbar(any(), anyInt(), any(), onClickListenerCaptor.capture())
        onClickListenerCaptor.firstValue.onClick(mock())

        // Then
        verify(eventTracker).onAnalysisScreenEvent(Event(AnalysisScreenEvent.RETRY))
    }

    @Test
    @Throws(Exception::class)
    fun should_notifyListener_ofError_whenGiniInstanceIsMissing() {
        // Given
        val presenter = createPresenter(ImageDocumentFake(), null)
        val listener = mock<AnalysisFragmentListener>()
        presenter.setListener(listener)

        // When
        presenter.start()

        // Then
        val args = argumentCaptor<GiniCaptureError>()
        verify(listener).onError(args.capture())
        assertThat(args.firstValue.errorCode)
            .isEqualTo(GiniCaptureError.ErrorCode.MISSING_GINI_CAPTURE_INSTANCE)
    }
}