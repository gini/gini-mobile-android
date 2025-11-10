package net.gini.android.capture.di

import net.gini.android.capture.paymentHints.GetAlreadyPaidHintEnabledUseCase
import org.koin.dsl.module

internal val paymentHintsModule = module {

    factory {
        GetAlreadyPaidHintEnabledUseCase(
            giniBankConfigurationProvider = get(),
        )
    }

}
