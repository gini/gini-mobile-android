package net.gini.android.bank.sdk.capture.digitalinvoice.skonto

import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import net.gini.android.capture.Amount
import java.math.BigDecimal
import java.time.LocalDate

internal sealed class DigitalInvoiceSkontoScreenState {

    data class Ready(
        val isSkontoSectionActive: Boolean,
        val paymentInDays: Int,
        val skontoPercentage: BigDecimal,
        val skontoAmount: Amount,
        val fullAmount: Amount,
        val discountDueDate: LocalDate,
        val paymentMethod: SkontoData.SkontoPaymentMethod,
        val edgeCase: SkontoEdgeCase?,
        val edgeCaseInfoDialogVisible: Boolean,
    ) : DigitalInvoiceSkontoScreenState()
}

internal sealed interface DigitalInvoiceSkontoSideEffect {
    data class OpenInvoiceScreen(val documentId: String, val skontoData: SkontoData) :
        DigitalInvoiceSkontoSideEffect

    object OpenHelpScreen : DigitalInvoiceSkontoSideEffect
}

