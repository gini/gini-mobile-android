package net.gini.android.capture.di

import net.gini.android.capture.einvoice.GetEInvoiceFeatureEnabledUseCase
import org.koin.dsl.module

internal val EInvoiceModule = module {

    factory {
        GetEInvoiceFeatureEnabledUseCase(
            giniBankConfigurationProvider = get(),
        )
    }

}