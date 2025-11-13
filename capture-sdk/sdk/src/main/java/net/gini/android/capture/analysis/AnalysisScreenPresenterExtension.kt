package net.gini.android.capture.analysis

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.gini.android.capture.BankSDKBridge
import net.gini.android.capture.BankSDKProperties
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.analysis.AnalysisInteractor.ResultHolder
import net.gini.android.capture.analysis.AnalysisScreenContract.View
import net.gini.android.capture.analysis.transactiondoc.AttachedToTransactionDocumentProvider
import net.gini.android.capture.analysis.warning.WarningType
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.document.GiniCaptureDocument
import net.gini.android.capture.document.GiniCaptureDocumentError
import net.gini.android.capture.document.GiniCaptureMultiPageDocument
import net.gini.android.capture.internal.qreducation.GetInvoiceEducationTypeUseCase
import net.gini.android.capture.internal.qreducation.IncrementInvoiceRecognizedCounterUseCase
import net.gini.android.capture.internal.qreducation.model.InvoiceEducationType
import net.gini.android.capture.internal.util.NullabilityHelper.getListOrEmpty
import net.gini.android.capture.internal.util.NullabilityHelper.getMapOrEmpty
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.paymentHints.GetAlreadyPaidHintEnabledUseCase
import net.gini.android.capture.paymentHints.GetPaymentDueHintEnabledUseCase
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.EventTrackingHelper

internal class AnalysisScreenPresenterExtension(
    private val view: View
) {

    var listener: AnalysisFragmentListener? = null

    var bankSDKBridge: BankSDKBridge? = null

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

    private val educationMutex = Mutex()

    private var invoiceEducationType: InvoiceEducationType? = null

    fun getAnalysisFragmentListenerOrNoOp(): AnalysisFragmentListener {
        return listener ?: noOpListener
    }

    fun isRAOrSkontoIncludedInExtractions(resultHolder: ResultHolder): Boolean {
        val bankSDKProperties: BankSDKProperties? =
            bankSDKBridge?.getBankSDKProperties(
                ResultHolder.toCaptureResult(
                    resultHolder
                )
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

    fun proceedSuccessNoExtractions(
        document: GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
    ) {
        doWhenEducationFinished {
            EventTrackingHelper.trackAnalysisScreenEvent(AnalysisScreenEvent.NO_RESULTS)
            getAnalysisFragmentListenerOrNoOp()
                .onProceedToNoExtractionsScreen(document)
        }
    }

    fun proceedWithExtractionsWhenEducationFinished(resultHolder: AnalysisInteractor.ResultHolder) {
        doWhenEducationFinished {
            proceedWithExtractions(resultHolder)
        }
    }

    fun proceedWithExtractions(resultHolder: AnalysisInteractor.ResultHolder) {
        getAnalysisFragmentListenerOrNoOp()
            .onExtractionsAvailable(
                getMapOrEmpty(resultHolder.extractions),
                getMapOrEmpty(resultHolder.compoundExtractions),
                getListOrEmpty(resultHolder.returnReasons)
            )

    }

    fun showAlreadyPaidHint(resultHolder: AnalysisInteractor.ResultHolder) {
        doWhenEducationFinished {
            view.showAlreadyPaidWarning(
                WarningType.DOCUMENT_MARKED_AS_PAID,
                { proceedWithExtractions(resultHolder) })
        }
    }

    fun showPaymentDueHint(
        resultHolder: AnalysisInteractor.ResultHolder,
        dueDate: String
    ) {
        doWhenEducationFinished {
            view.showPaymentDueHint(
                { proceedWithExtractions(resultHolder) },
                dueDate
            )

        }
    }

    fun getInvoiceEducationType(): InvoiceEducationType? {
        runBlocking {
            invoiceEducationType =
                runCatching { getInvoiceEducationTypeUseCase.execute() }.getOrNull()
        }
        return invoiceEducationType
    }

    fun showLoadingIndicator(
        onEducationFlowTriggered: () -> Unit
    ) = runBlocking {
        if (getInvoiceEducationType() != null) {
            view.showEducation {
                runBlocking { incrementInvoiceRecognizedCounterUseCase.execute() }
                educationMutex.unlock()
            }
            educationMutex.lock()
            onEducationFlowTriggered()
        }
    }

    fun releaseMutex() {
        if (educationMutex.isLocked) educationMutex.unlock()
    }

    private fun doWhenEducationFinished(action: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            educationMutex.withLock {
                withContext(Dispatchers.Main) {
                    action()
                }
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
