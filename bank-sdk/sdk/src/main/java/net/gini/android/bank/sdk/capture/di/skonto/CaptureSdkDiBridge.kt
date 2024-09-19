package net.gini.android.bank.sdk.capture.di.skonto

import kotlinx.coroutines.Dispatchers
import net.gini.android.bank.sdk.di.getGiniBankKoin
import net.gini.android.bank.sdk.transactiondocs.internal.provider.document.AttachedToTransactionDocumentProviderImpl
import net.gini.android.bank.sdk.transactiondocs.internal.provider.view.AttachDocToTransactionDialogProviderImpl
import net.gini.android.capture.analysis.transactiondoc.AttachDocToTransactionDialogProvider
import net.gini.android.capture.analysis.transactiondoc.AttachedToTransactionDocumentProvider
import org.koin.dsl.module

val captureSdkDiBridge = module {

    factory<AttachDocToTransactionDialogProvider> {
        AttachDocToTransactionDialogProviderImpl(
            giniTransactionDocsSettings = getGiniBankKoin().get()
        )
    }

    single<AttachedToTransactionDocumentProvider> {
        AttachedToTransactionDocumentProviderImpl(Dispatchers.IO)
    }
}
