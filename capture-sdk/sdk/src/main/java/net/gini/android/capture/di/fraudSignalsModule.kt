package net.gini.android.capture.di

import net.gini.android.capture.fraudSignals.FraudSignalsParser
import net.gini.android.capture.fraudSignals.GetFraudSignalsEnabledUseCase
import org.koin.dsl.module

internal val fraudSignalsModule = module {

    factory {
        GetFraudSignalsEnabledUseCase(
            giniBankConfigurationProvider = get(),
        )
    }

    factory {
        FraudSignalsParser(
            getFraudSignalsEnabled = get(),
        )
    }
}
