package net.gini.android.bank.sdk.capture.digitalinvoice.skonto

import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.args.DigitalInvoiceSkontoArgs
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.validation.DigitalInvoiceSkontoAmountValidator
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val digitalInvoiceSkontoScreenModule = module {
    viewModel { (data: DigitalInvoiceSkontoArgs) ->
        DigitalInvoiceSkontoViewModel(
            args = data,
            getSkontoDiscountPercentageUseCase = get(),
            getSkontoEdgeCaseUseCase = get(),
            getSkontoRemainingDaysUseCase = get(),
            lastAnalyzedDocumentProvider = get(),
            skontoInvoicePreviewTextLinesFactory = get(),
            digitalInvoiceSkontoAmountValidator = get(),
        )
    }
    factory { DigitalInvoiceSkontoAmountValidator() }
}
