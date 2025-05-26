package net.gini.android.capture.analysis

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.analysis.AnalysisScreenContract.View
import net.gini.android.capture.analysis.transactiondoc.AttachedToTransactionDocumentProvider
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.document.GiniCaptureDocument
import net.gini.android.capture.document.GiniCaptureDocumentError
import net.gini.android.capture.document.GiniCaptureMultiPageDocument
import net.gini.android.capture.internal.qreducation.GetInvoiceEducationTypeUseCase
import net.gini.android.capture.internal.qreducation.IncrementInvoiceRecognizedCounterUseCase
import net.gini.android.capture.internal.util.NullabilityHelper.getListOrEmpty
import net.gini.android.capture.internal.util.NullabilityHelper.getMapOrEmpty
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.EventTrackingHelper

internal class AnalysisScreenPresenterExtension(
    private val view: View
) {

    var listener: AnalysisFragmentListener? = null


    val lastAnalyzedDocumentProvider: LastAnalyzedDocumentProvider
            by getGiniCaptureKoin().inject()

    val attachDocToTransactionDialogProvider: AttachedToTransactionDocumentProvider
            by getGiniCaptureKoin().inject()

    private val getInvoiceEducationTypeUseCase: GetInvoiceEducationTypeUseCase
            by getGiniCaptureKoin().inject()
    private val incrementInvoiceRecognizedCounterUseCase: IncrementInvoiceRecognizedCounterUseCase
            by getGiniCaptureKoin().inject()

    private val educationMutex = Mutex()

    fun getAnalysisFragmentListenerOrNoOp(): AnalysisFragmentListener {
        return listener ?: noOpListener
    }

    fun proceedSuccessNoExtractions(
        document: GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
    ) {
        doWhenEducationFinished {
            EventTrackingHelper.trackAnalysisScreenEvent(AnalysisScreenEvent.NO_RESULTS)
            getAnalysisFragmentListenerOrNoOp()
                .onProceedToNoExtractionsScreen(document)
        }
    }

    fun proceedWithExtractions(resultHolder: AnalysisInteractor.ResultHolder) {
        doWhenEducationFinished {
            getAnalysisFragmentListenerOrNoOp()
                .onExtractionsAvailable(
                    getMapOrEmpty(resultHolder.extractions),
                    getMapOrEmpty(resultHolder.compoundExtractions),
                    getListOrEmpty(resultHolder.returnReasons)
                )
        }
    }

    fun showLoadingIndicator(
        onEducationFlowTriggered: () -> Unit
    ) = runBlocking {
        val type = getInvoiceEducationTypeUseCase.execute()
        if (type != null) {
            view.showEducation {
                runBlocking { incrementInvoiceRecognizedCounterUseCase.execute() }
                educationMutex.unlock()
            }
            educationMutex.lock()
            onEducationFlowTriggered()
        }
    }

    private fun doWhenEducationFinished(action: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            educationMutex.withLock {
                action()
            }
        }
    }

    private val noOpListener: AnalysisFragmentListener = object : AnalysisFragmentListener {

        override fun onError(error: GiniCaptureError) {
            /* no-op */
        }

        override fun onExtractionsAvailable(
            extractions: Map<String, GiniCaptureSpecificExtraction>,
            compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
            returnReasons: List<GiniCaptureReturnReason>
        ) {
            /* no-op */
        }

        override fun onProceedToNoExtractionsScreen(document: Document) {
            /* no-op */
        }

        override fun onDefaultPDFAppAlertDialogCancelled() {
            /* no-op */
        }
    }
}
