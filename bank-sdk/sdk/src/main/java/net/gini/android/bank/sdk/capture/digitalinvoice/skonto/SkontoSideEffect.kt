package net.gini.android.bank.sdk.capture.digitalinvoice.skonto

import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.args.DigitalInvoiceSkontoResultArgs

internal sealed interface SkontoSideEffect {
    data class OpenInvoiceScreen(val documentId: String, val infoTextLines: List<String>) :
        SkontoSideEffect

    object OpenHelpScreen : SkontoSideEffect

    data class NavigateBack(val args: DigitalInvoiceSkontoResultArgs) : SkontoSideEffect
}
