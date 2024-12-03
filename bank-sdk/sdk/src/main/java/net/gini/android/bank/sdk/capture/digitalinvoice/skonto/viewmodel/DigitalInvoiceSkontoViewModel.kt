package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel

import androidx.lifecycle.ViewModel
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoSideEffect
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.args.DigitalInvoiceSkontoArgs
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.BackClickIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.InfoBannerInteractionIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.InvoiceClickIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.KeyboardStateChangeIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.SkontoAmountFieldChangeIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.SkontoDueDateChangeIntent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import java.math.BigDecimal
import java.time.LocalDate

internal typealias SkontoContainerHost =
        ContainerHost<SkontoScreenState, SkontoSideEffect>

internal class DigitalInvoiceSkontoViewModel(
    args: DigitalInvoiceSkontoArgs,
    skontoScreenInitialStateFactory: SkontoScreenInitialStateFactory,
    private val invoiceClickIntent: InvoiceClickIntent,
    private val backClickIntent: BackClickIntent,
    private val infoBannerInteractionIntent: InfoBannerInteractionIntent,
    private val keyboardStateChangeIntent: KeyboardStateChangeIntent,
    private val skontoDueDateChangeIntent: SkontoDueDateChangeIntent,
    private val skontoAmountFieldChangeIntent: SkontoAmountFieldChangeIntent,
) : ViewModel(), SkontoContainerHost {

    override val container: Container<SkontoScreenState, SkontoSideEffect> =
        container(skontoScreenInitialStateFactory.create(args.data, args.isSkontoSectionActive))

    fun onSkontoAmountFieldChanged(newValue: BigDecimal) =
        with(skontoAmountFieldChangeIntent) { run(newValue) }

    fun onSkontoDueDateChanged(newDate: LocalDate) =
        with(skontoDueDateChangeIntent) { run(newDate) }

    fun onKeyboardStateChanged(isVisible: Boolean) =
        with(keyboardStateChangeIntent) { run(isVisible) }

    fun onInfoBannerClicked() =
        with(infoBannerInteractionIntent) { runClick() }

    fun onInfoDialogDismissed() =
        with(infoBannerInteractionIntent) { runDismiss() }

    fun onInvoiceClicked() =
        with(invoiceClickIntent) { run() }

    fun onBackClicked() =
        with(backClickIntent) { run() }


    fun onHelpClicked() =
        intent { postSideEffect(SkontoSideEffect.OpenHelpScreen) }
}
