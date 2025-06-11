package net.gini.android.capture.di

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
            flowTypeStorage = get()
        )
    }
    factory {
        IncrementInvoiceRecognizedCounterUseCase(
            invoiceEducationStorage = get()
        )
    }
}
