package net.gini.android.bank.sdk.transactiondocs.di

import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.transactiondocs.internal.GiniBankTransactionDocs
import net.gini.android.bank.sdk.transactiondocs.internal.GiniTransactionDocsSettings
import net.gini.android.bank.sdk.transactiondocs.internal.repository.GiniAttachTransactionDocDialogDecisionRepository
import net.gini.android.bank.sdk.transactiondocs.internal.factory.TransactionDocInvoicePreviewInfoLinesFactory
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.GetTransactionDocShouldBeAutoAttachedUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.TransactionDocDialogCancelAttachUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.TransactionDocDialogConfirmAttachUseCase
import net.gini.android.capture.analysis.transactiondoc.AttachedToTransactionDocumentProvider
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.provider.LastExtractionsProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val transactionListModule = module {
    factory<GiniBankTransactionDocs?> { GiniBank.giniBankTransactionDocs }
    factory<GiniBankTransactionDocs> { GiniBank.giniBankTransactionDocs!! }

    single<GiniTransactionDocsSettings> {
        GiniTransactionDocsSettings(
            context = androidContext()
        )
    }

    factory {
        TransactionDocInvoicePreviewInfoLinesFactory(
            resources = androidContext().resources,
            lastExtractionsProvider = get<LastExtractionsProvider>(),
            amountFormatter = get(),
        )
    }

    factory {
        TransactionDocDialogCancelAttachUseCase(
            attachTransactionDocDialogDecisionRepository = get()
        )
    }

    factory {
        TransactionDocDialogConfirmAttachUseCase(
            giniTransactionDocsSettings = get(),
            attachTransactionDocDialogDecisionRepository = get()
        )
    }

    factory {
        GetTransactionDocShouldBeAutoAttachedUseCase(
            giniTransactionDocsSettings = get(),
        )
    }

    single { GiniAttachTransactionDocDialogDecisionRepository() }

    // Bridge between GiniCapture and GiniBank
    factory<AttachedToTransactionDocumentProvider> { getGiniCaptureKoin().get() }

    // Bridge between GiniCapture and GiniBank
    factory<LastExtractionsProvider> { getGiniCaptureKoin().get() }
}
