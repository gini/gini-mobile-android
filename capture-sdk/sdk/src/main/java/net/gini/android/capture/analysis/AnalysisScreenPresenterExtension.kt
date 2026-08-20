package net.gini.android.capture.analysis

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import net.gini.android.capture.internal.storage.ImageDiskStore
import net.gini.android.capture.internal.util.NullabilityHelper.getListOrEmpty
import net.gini.android.capture.internal.util.NullabilityHelper.getMapOrEmpty
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.paymentHints.GetAlreadyPaidHintEnabledUseCase
import net.gini.android.capture.paymentHints.GetCreditNoteHintEnabledUseCase
import net.gini.android.capture.paymentHints.GetPaymentDueHintEnabledUseCase
import net.gini.android.capture.paymentHints.GetPaymentScheduleHintEnabledUseCase
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

    val creditNoteHintEnabledUseCase:
            GetCreditNoteHintEnabledUseCase by getGiniCaptureKoin().inject()

    val paymentScheduleHintEnabledUseCase:
            GetPaymentScheduleHintEnabledUseCase by getGiniCaptureKoin().inject()

    val lastAnalyzedDocumentProvider: LastAnalyzedDocumentProvider
            by getGiniCaptureKoin().inject()

    val attachDocToTransactionDialogProvider: AttachedToTransactionDocumentProvider
            by getGiniCaptureKoin().inject()
    private val getInvoiceEducationTypeUseCase: GetInvoiceEducationTypeUseCase
            by getGiniCaptureKoin().inject()
    private val incrementInvoiceRecognizedCounterUseCase: IncrementInvoiceRecognizedCounterUseCase
            by getGiniCaptureKoin().inject()

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val educationMutex = Mutex()

    private var invoiceEducationType: InvoiceEducationType? = null

    /**
     * Which CTA is waiting for the local invoice saving step to finish. Mirrored into the view's
     * saved state so it survives a recreation while the SAF folder picker is open.
     */
    private var pendingSavingAction: PendingSavingAction = PendingSavingAction.PROCEED

    fun cancel() {
        job.cancel()
    }

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

    fun proceedWithExtractionsWhenEducationFinished(
        resultHolder: ResultHolder,
        mIsInvoiceSavingEnabled: Boolean,
        isSavingInvoicesInProgress: Boolean,
        activity: Activity
    ) {
        if (isSavingInvoicesInProgress) {
            handleSaveInvoicesLocally(
                mIsInvoiceSavingEnabled,
                true,
                resultHolder,
                activity
            )
        } else {
            doWhenEducationFinished {
                handleSaveInvoicesLocally(
                    mIsInvoiceSavingEnabled,
                    false,
                    resultHolder,
                    activity
                )
            }
        }
    }

    fun proceedWithExtractions(resultHolder: ResultHolder) {
        getAnalysisFragmentListenerOrNoOp()
            .onExtractionsAvailable(
                getMapOrEmpty(resultHolder.extractions),
                getMapOrEmpty(resultHolder.compoundExtractions),
                getListOrEmpty(resultHolder.returnReasons)
            )
    }

    /**
     * Shows the already-paid or credit-note warning for [warningType], running the local invoice
     * saving step before continuing. The two warnings share this flow; only the view method
     * showing the bottom sheet differs.
     */
    fun showDocumentMarkedWarning(
        warningType: WarningType,
        mIsInvoiceSavingEnabled: Boolean,
        isSavingInvoicesInProgress: Boolean,
        resultHolder: ResultHolder,
        activity: Activity
    ) {
        if (isSavingInvoicesInProgress) {
            handleSaveInvoicesLocally(
                mIsInvoiceSavingEnabled,
                true,
                resultHolder,
                activity
            )
        } else {
            doWhenEducationFinished {
                val onProceed = Runnable {
                    handleSaveInvoicesLocally(
                        mIsInvoiceSavingEnabled,
                        false,
                        resultHolder,
                        activity
                    )
                }
                if (warningType == WarningType.DOCUMENT_MARKED_AS_CREDIT_NOTE) {
                    view.showCreditNoteWarning(warningType, onProceed)
                } else {
                    view.showAlreadyPaidWarning(warningType, onProceed)
                }
            }
        }
    }

    /**
     * Runs the local invoice saving step, then continues with [pendingAction].
     *
     * Saving hands off to the Storage Access Framework, so the action cannot be run here. It is
     * handed to the view, which persists it across a recreation (rotation, low memory) and gives
     * it back through [restorePendingSavingAction]. [resumeAfterInvoiceSaving] then runs it once
     * the files have been written.
     */
    private fun handleSaveInvoicesLocally(
        mIsInvoiceSavingEnabled: Boolean,
        isSavingInvoicesInProgress: Boolean,
        resultHolder: ResultHolder,
        activity: Activity,
        pendingAction: PendingSavingAction = PendingSavingAction.PROCEED
    ) {
        if (!mIsInvoiceSavingEnabled || isSavingInvoicesInProgress) {
            runPendingSavingAction(pendingAction, resultHolder, activity)
            return
        } else {
            pendingSavingAction = pendingAction
            view.processInvoiceSaving(pendingAction.name)
        }
    }

    /**
     * Continues the flow after the local invoice saving step finished, with whichever CTA started
     * it.
     */
    fun resumeAfterInvoiceSaving(
        resultHolder: ResultHolder,
        activity: Activity
    ) {
        val action = pendingSavingAction
        pendingSavingAction = PendingSavingAction.PROCEED
        runPendingSavingAction(action, resultHolder, activity)
    }

    private fun runPendingSavingAction(
        action: PendingSavingAction,
        resultHolder: ResultHolder,
        activity: Activity
    ) = when (action) {
        PendingSavingAction.PROCEED -> clearSavedImagesAndProceed(resultHolder, activity)
        PendingSavingAction.SCHEDULE_PAYMENT -> proceedWithSchedulePayment(resultHolder, activity)
    }

    /**
     * Restores the CTA that started the local invoice saving step after the screen was recreated.
     *
     * Called with the value the view persisted. An unknown or missing name falls back to
     * [PendingSavingAction.PROCEED], which is the behavior that existed before the scheduled
     * payment state — so the due date hint and the already-paid warning are unaffected.
     */
    fun restorePendingSavingAction(actionName: String?) {
        pendingSavingAction = PendingSavingAction.entries
            .firstOrNull { it.name == actionName }
            ?: PendingSavingAction.PROCEED
    }

    /**
     * Which CTA is waiting for the local invoice saving step to finish. Persisted by the view,
     * because saving leaves the SDK for the Storage Access Framework and the screen can be
     * recreated while that is open.
     */
    internal enum class PendingSavingAction {
        /** Continue the pay-now flow — "Proceed Anyway", and every state that has no other CTA. */
        PROCEED,

        /** Hand the extractions to the bank app — the scheduled payment state's primary CTA. */
        SCHEDULE_PAYMENT
    }

    fun clearSavedImagesAndProceed(
        resultHolder: ResultHolder,
        activity: Activity
    ) {
        ImageDiskStore.clear(activity)
        proceedWithExtractions(resultHolder)
    }

    fun showPaymentDueHint(
        resultHolder: ResultHolder,
        dueDate: String,
        mIsInvoiceSavingEnabled: Boolean,
        isSavingInvoicesInProgress: Boolean,
        activity: Activity
    ) {
        if (isSavingInvoicesInProgress) {
            handleSaveInvoicesLocally(
                mIsInvoiceSavingEnabled,
                true,
                resultHolder,
                activity
            )
        } else {
            doWhenEducationFinished {
                view.showPaymentDueHint(
                    DueDateFormatter.formatToLocalStyle(dueDate)
                ) {
                    handleSaveInvoicesLocally(
                        mIsInvoiceSavingEnabled,
                        false,
                        resultHolder,
                        activity
                    )
                }
            }
        }
    }

    fun showSchedulePaymentHint(
        resultHolder: ResultHolder,
        dueDate: String,
        mIsInvoiceSavingEnabled: Boolean,
        isSavingInvoicesInProgress: Boolean,
        activity: Activity
    ) {
        if (isSavingInvoicesInProgress) {
            // Saving was already running when the screen was recreated. The CTA the user actually
            // tapped was restored into pendingSavingAction, so continue with that one instead of
            // assuming pay-now.
            handleSaveInvoicesLocally(
                mIsInvoiceSavingEnabled,
                true,
                resultHolder,
                activity,
                pendingSavingAction
            )
        } else {
            doWhenEducationFinished {
                view.showSchedulePaymentHint(
                    DueDateFormatter.formatToLocalStyle(dueDate),
                    {
                        handleSaveInvoicesLocally(
                            mIsInvoiceSavingEnabled,
                            false,
                            resultHolder,
                            activity,
                            PendingSavingAction.PROCEED
                        )
                    },
                    {
                        // The invoice is saved locally first, exactly like the due date state's
                        // "Proceed Anyway", and only then handed off to the bank app.
                        handleSaveInvoicesLocally(
                            mIsInvoiceSavingEnabled,
                            false,
                            resultHolder,
                            activity,
                            PendingSavingAction.SCHEDULE_PAYMENT
                        )
                    }
                )
            }
        }
    }

    /**
     * Hands the extractions to the hosting app so it can open its own scheduled transfer flow.
     * The saved images are cleared just like on the pay-now path, but the flow finishes through
     * [AnalysisFragmentListener.onSchedulePayment] instead of `onExtractionsAvailable`.
     */
    fun proceedWithSchedulePayment(
        resultHolder: ResultHolder,
        activity: Activity
    ) {
        ImageDiskStore.clear(activity)
        getAnalysisFragmentListenerOrNoOp()
            .onSchedulePayment(
                getMapOrEmpty(resultHolder.extractions),
                getMapOrEmpty(resultHolder.compoundExtractions),
                getListOrEmpty(resultHolder.returnReasons)
            )
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
        scope.launch {
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
