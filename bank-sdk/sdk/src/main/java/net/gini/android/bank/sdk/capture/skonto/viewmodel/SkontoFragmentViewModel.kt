package net.gini.android.bank.sdk.capture.skonto.viewmodel

import androidx.lifecycle.ViewModel
import net.gini.android.bank.sdk.capture.skonto.SkontoFragmentListener
import net.gini.android.bank.sdk.capture.skonto.SkontoScreenSideEffect
import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.FullAmountChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.InfoBannerInteractionIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.InvoiceClickIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.KeyboardStateChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.ProceedClickedIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.SkontoActiveChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.SkontoAmountFieldChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.SkontoDueDateChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.TransactionDocDialogDecisionIntent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import java.math.BigDecimal
import java.time.LocalDate

internal typealias SkontoScreenContainerHost = ContainerHost<SkontoScreenState, SkontoScreenSideEffect>

internal class SkontoFragmentViewModel(
    data: SkontoData,
    skontoScreenInitialStateFactory: SkontoScreenInitialStateFactory,

    private val proceedClickedIntent: ProceedClickedIntent,
    private val skontoActiveChangeIntent: SkontoActiveChangeIntent,
    private val keyboardStateChangeIntent: KeyboardStateChangeIntent,
    private val skontoAmountFieldChangeIntent: SkontoAmountFieldChangeIntent,
    private val invoiceClickIntent: InvoiceClickIntent,
    private val fullAmountChangeIntent: FullAmountChangeIntent,
    private val skontoDueDateChangeIntent: SkontoDueDateChangeIntent,
    private val transactionDocDialogDecisionIntent: TransactionDocDialogDecisionIntent,
    private val infoBannerInteractionIntent: InfoBannerInteractionIntent,
) : ViewModel(), SkontoScreenContainerHost {

    override val container: Container<SkontoScreenState, SkontoScreenSideEffect> = container(
        skontoScreenInitialStateFactory.create(data)
    )

    private var listener: SkontoFragmentListener? = null

    fun setListener(listener: SkontoFragmentListener?) {
        this.listener = listener
    }

    fun onProceedClicked() =
        with(proceedClickedIntent) { run(listener) }

    fun onConfirmAttachTransactionDocClicked(alwaysAttach: Boolean) =
        with(transactionDocDialogDecisionIntent) { runConfirm(alwaysAttach, listener) }

    fun onCancelAttachTransactionDocClicked() =
        with(transactionDocDialogDecisionIntent) { runCancel(listener) }

    fun onSkontoActiveChanged(newValue: Boolean) =
        with(skontoActiveChangeIntent) { run(newValue) }

    fun onKeyboardStateChanged(isVisible: Boolean) =
        with(keyboardStateChangeIntent) { run(isVisible) }

    fun onSkontoAmountFieldChanged(newValue: BigDecimal) =
        with(skontoAmountFieldChangeIntent) { run(newValue) }

    fun onSkontoDueDateChanged(newDate: LocalDate) =
        with(skontoDueDateChangeIntent) { run(newDate) }

    fun onFullAmountFieldChanged(newValue: BigDecimal) =
        with(fullAmountChangeIntent) { run(newValue) }

    fun onInfoBannerClicked() =
        with(infoBannerInteractionIntent) { runClick() }

    fun onInfoDialogDismissed() =
        with(infoBannerInteractionIntent) { runDismiss() }

    fun onInvoiceClicked() =
        with(invoiceClickIntent) { run() }
}
