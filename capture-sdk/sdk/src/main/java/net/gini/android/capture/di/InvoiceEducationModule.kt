package net.gini.android.capture.di

import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.qreducation.GetInvoiceEducationTypeUseCase
import net.gini.android.capture.internal.qreducation.IncrementInvoiceRecognizedCounterUseCase
import net.gini.android.capture.internal.storage.InvoiceEducationStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val invoiceEducationModule = module {
    single {
        InvoiceEducationStorage(
            context = androidContext()
        )
    }
    factory {
        GetInvoiceEducationTypeUseCase(
            invoiceEducationStorage = get(),
            documentImportEnabledFileTypesProvider = {
                runCatching { GiniCapture.getInstance().documentImportEnabledFileTypes }.getOrNull()
            },
            flowTypeStorage = get()
        )
    }
    factory {
        IncrementInvoiceRecognizedCounterUseCase(
            invoiceEducationStorage = get()
        )
    }
}
