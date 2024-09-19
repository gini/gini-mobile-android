package net.gini.android.bank.sdk.transactiondocs.di

import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.transactiondocs.internal.GiniBankTransactionDocs
import net.gini.android.bank.sdk.transactiondocs.internal.GiniTransactionDocsSettings
import net.gini.android.bank.sdk.transactiondocs.internal.TransactionDocInvoicePreviewInfoLinesFactory
import net.gini.android.capture.analysis.transactiondoc.AttachedToTransactionDocumentProvider
import net.gini.android.capture.di.getGiniCaptureKoin
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
            resources = androidContext().resources
        )
    }

    // Bridge between GiniCapture and GiniBank
    factory<AttachedToTransactionDocumentProvider> { getGiniCaptureKoin().get() }
}
