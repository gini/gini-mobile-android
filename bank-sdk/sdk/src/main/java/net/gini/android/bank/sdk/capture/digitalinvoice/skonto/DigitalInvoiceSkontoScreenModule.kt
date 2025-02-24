package net.gini.android.bank.sdk.capture.digitalinvoice.skonto

import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.args.DigitalInvoiceSkontoArgs
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.validation.DigitalInvoiceSkontoAmountValidator
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.DigitalInvoiceSkontoViewModel
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.SkontoScreenInitialStateFactory
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.BackClickIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.InfoBannerInteractionIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.InvoiceClickIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.KeyboardStateChangeIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.SkontoAmountFieldChangeIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.SkontoDueDateChangeIntent
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val digitalInvoiceSkontoScreenModule = module {
    viewModel { (data: DigitalInvoiceSkontoArgs) ->
        DigitalInvoiceSkontoViewModel(
            args = data,
            skontoScreenInitialStateFactory = get(),
            invoiceClickIntent = get(),
            backClickIntent = get(),
            infoBannerInteractionIntent = get(),
            keyboardStateChangeIntent = get(),
            skontoDueDateChangeIntent = get(),
            skontoAmountFieldChangeIntent = get()
        )
    }
    factory { DigitalInvoiceSkontoAmountValidator() }
    factory {
        SkontoScreenInitialStateFactory(
            getSkontoEdgeCaseUseCase = get()
        )
    }
    factory {
        InvoiceClickIntent(
            lastAnalyzedDocumentProvider = get(),
            skontoInvoicePreviewTextLinesFactory = get(),
        )
    }
    factory { BackClickIntent() }
    factory { InfoBannerInteractionIntent() }
    factory { KeyboardStateChangeIntent() }
    factory {
        SkontoDueDateChangeIntent(
            getSkontoRemainingDaysUseCase = get(),
            getSkontoEdgeCaseUseCase = get(),
        )
    }
    factory {
        SkontoAmountFieldChangeIntent(
            digitalInvoiceSkontoAmountValidator = get(),
            getSkontoDiscountPercentageUseCase = get(),
        )
    }

}
