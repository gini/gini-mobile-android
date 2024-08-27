package net.gini.android.bank.sdk.di

import net.gini.android.bank.sdk.capture.extractions.skonto.SkontoExtractionsHandler
import org.koin.dsl.module

val handlerModule = module {
    single {
        SkontoExtractionsHandler()
    }
}
