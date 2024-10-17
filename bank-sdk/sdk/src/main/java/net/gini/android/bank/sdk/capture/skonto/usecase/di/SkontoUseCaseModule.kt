package net.gini.android.bank.sdk.capture.skonto.usecase.di

import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoAmountUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDefaultSelectionStateUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDiscountPercentageUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoEdgeCaseUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoRemainingDaysUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoSavedAmountUseCase
import org.koin.dsl.module

val skontoUseCaseModule = module {
    factory { GetSkontoAmountUseCase() }
    factory { GetSkontoDiscountPercentageUseCase() }
    factory { GetSkontoEdgeCaseUseCase() }
    factory { GetSkontoSavedAmountUseCase() }
    factory { GetSkontoDefaultSelectionStateUseCase() }
    factory { GetSkontoRemainingDaysUseCase() }
}
