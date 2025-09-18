package net.gini.android.capture.di

import net.gini.android.capture.paymentHints.GetPaymentHintsEnabledUseCase
import org.koin.dsl.module

internal val PaymentHintsModule = module {

    factory {
        GetPaymentHintsEnabledUseCase(
            giniBankConfigurationProvider = get(),
        )
    }

}
