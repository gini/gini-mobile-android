package net.gini.android.bank.sdk.capture.skonto

import net.gini.android.bank.sdk.capture.skonto.factory.lines.SkontoInvoicePreviewTextLinesFactory
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.validation.SkontoAmountValidator
import net.gini.android.bank.sdk.capture.skonto.validation.SkontoFullAmountValidator
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoFragmentViewModel
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenAnalytics
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenInitialStateFactory
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.FullAmountChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.InfoBannerInteractionIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.InvoiceClickIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.KeyboardStateChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.ProceedClickedIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.SkontoActiveChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.SkontoAmountFieldChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.SkontoDueDateChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.TransactionDocDialogDecisionIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.subintent.OpenExtractionsScreenSubIntent
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val skontoScreenModule = module {
    viewModel { (data: SkontoData) ->
        SkontoFragmentViewModel(
            data = data,
            skontoScreenInitialStateFactory = get(),
            proceedClickedIntent = get(),
            skontoActiveChangeIntent = get(),
            keyboardStateChangeIntent = get(),
            skontoAmountFieldChangeIntent = get(),
            invoiceClickIntent = get(),
            fullAmountChangeIntent = get(),
            skontoDueDateChangeIntent = get(),
            transactionDocDialogDecisionIntent = get(),
            infoBannerInteractionIntent = get(),
            skontoScreenAnalytics = get(),
        )
    }
    factory {
        SkontoInvoicePreviewTextLinesFactory(
            resources = androidContext().resources,
            amountFormatter = get()
        )
    }
    factory {
        SkontoAmountValidator()
    }
    factory {
        SkontoFullAmountValidator()
    }
    factory {
        SkontoScreenInitialStateFactory(
            getSkontoSavedAmountUseCase = get(),
            getSkontoEdgeCaseUseCase = get(),
            getSkontoDefaultSelectionStateUseCase = get()
        )
    }
    factory {
        ProceedClickedIntent(
            openExtractionsScreenSubIntent = get(),
            getTransactionDocShouldBeAutoAttachedUseCase = get(),
            getTransactionDocsFeatureEnabledUseCase = get(),
            transactionDocDialogConfirmAttachUseCase = get(),
            analyticsTracker = get(),
        )
    }
    factory {
        InvoiceClickIntent(
            lastAnalyzedDocumentProvider = get(),
            skontoInvoicePreviewTextLinesFactory = get(),
            analyticsTracker = get(),
        )
    }
    factory {
        FullAmountChangeIntent(
            skontoFullAmountValidator = get(),
            getSkontoAmountUseCase = get(),
            getSkontoSavedAmountUseCase = get()
        )
    }
    factory {
        InfoBannerInteractionIntent()
    }
    factory {
        KeyboardStateChangeIntent()
    }
    factory {
        SkontoActiveChangeIntent(
            getSkontoDiscountPercentageUseCase = get(),
            analyticsTracker = get(),
        )
    }
    factory {
        SkontoAmountFieldChangeIntent(
            skontoAmountValidator = get(),
            getSkontoDiscountPercentageUseCase = get(),
            getSkontoSavedAmountUseCase = get(),
        )
    }
    factory {
        SkontoDueDateChangeIntent(
            getSkontoRemainingDaysUseCase = get(),
            getSkontoEdgeCaseUseCase = get(),
        )
    }
    factory {
        TransactionDocDialogDecisionIntent(
            openExtractionsScreenSubIntent = get(),
            transactionDocDialogConfirmAttachUseCase = get(),
            transactionDocDialogCancelAttachUseCase = get(),
        )
    }
    factory {
        OpenExtractionsScreenSubIntent(
            skontoExtractionsHandler = get(),
            lastExtractionsProvider = get()
        )
    }
    factory {
        SkontoScreenAnalytics(
            analyticsTracker = get(),
        )
    }
}