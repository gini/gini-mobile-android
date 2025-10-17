package net.gini.android.capture.di

import net.gini.android.capture.saveinvoiceslocally.GetSaveInvoicesLocallyFeatureEnabledUseCase
import org.koin.dsl.module

internal val saveInvoicesLocallyModule = module {

    factory {
        GetSaveInvoicesLocallyFeatureEnabledUseCase(
            giniBankConfigurationProvider = get(),
        )
    }
}
