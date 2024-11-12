package net.gini.android.bank.sdk.capture.digitalinvoice.skonto

import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import net.gini.android.capture.Amount
import java.math.BigDecimal
import java.time.LocalDate

internal sealed interface DigitalInvoiceSkontoScreenState {

    data class Ready(
        val isSkontoSectionActive: Boolean,
        val paymentInDays: Int,
        val skontoPercentage: BigDecimal,
        val skontoAmount: Amount,
        val skontoAmountValidationError: SkontoAmountValidationError?,
        val fullAmount: Amount,
        val discountDueDate: LocalDate,
        val paymentMethod: SkontoData.SkontoPaymentMethod,
        val edgeCase: SkontoEdgeCase?,
        val edgeCaseInfoDialogVisible: Boolean,
    ) : DigitalInvoiceSkontoScreenState {

        sealed interface SkontoAmountValidationError {
            object SkontoAmountMoreThanFullAmount : SkontoAmountValidationError
        }
    }
}

internal sealed interface DigitalInvoiceSkontoSideEffect {
    data class OpenInvoiceScreen(val documentId: String, val infoTextLines: List<String>) :
        DigitalInvoiceSkontoSideEffect

    object OpenHelpScreen : DigitalInvoiceSkontoSideEffect
}

