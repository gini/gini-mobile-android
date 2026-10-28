package net.gini.android.capture.di

import net.gini.android.capture.wero.GetWeroQRCodeScanningEnabledUseCase
import org.koin.dsl.module

internal val weroModule = module {

    factory {
        GetWeroQRCodeScanningEnabledUseCase(
            giniBankConfigurationProvider = get(),
        )
    }
}
