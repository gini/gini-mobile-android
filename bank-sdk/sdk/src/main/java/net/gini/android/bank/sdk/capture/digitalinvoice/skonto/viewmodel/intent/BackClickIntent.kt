package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoSideEffect
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.args.DigitalInvoiceSkontoResultArgs
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.SkontoContainerHost
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData

internal class BackClickIntent {

    fun SkontoContainerHost.run() = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent
        val args = DigitalInvoiceSkontoResultArgs(
            skontoData = SkontoData(
                skontoAmountToPay = state.skontoAmount,
                skontoDueDate = state.discountDueDate,
                skontoPercentageDiscounted = state.skontoPercentage,
                skontoRemainingDays = state.paymentInDays,
                fullAmountToPay = state.fullAmount,
                skontoPaymentMethod = state.paymentMethod,
            ),
            isSkontoEnabled = state.isSkontoSectionActive,
        )

        postSideEffect(SkontoSideEffect.NavigateBack(args))
    }
}
