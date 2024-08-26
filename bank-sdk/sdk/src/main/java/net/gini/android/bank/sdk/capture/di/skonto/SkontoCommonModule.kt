package net.gini.android.bank.sdk.capture.di.skonto

import net.gini.android.bank.sdk.capture.skonto.factory.text.SkontoDiscountLabelTextFactory
import net.gini.android.bank.sdk.capture.skonto.factory.text.SkontoInfoBannerTextFactory
import net.gini.android.bank.sdk.capture.skonto.factory.text.SkontoSavedAmountTextFactory
import net.gini.android.bank.sdk.capture.skonto.formatter.AmountFormatter
import net.gini.android.bank.sdk.capture.skonto.formatter.SkontoDiscountPercentageFormatter
import net.gini.android.bank.sdk.capture.skonto.formatter.SkontoRemainingDaysFormatter
import net.gini.android.bank.sdk.capture.util.currencyFormatterWithoutSymbol
import org.koin.android.ext.koin.androidContext
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val skontoCommonModule = module {
    factory {
        SkontoDiscountPercentageFormatter()
    }
    factory {
        SkontoRemainingDaysFormatter(resources = androidContext().resources)
    }
    factory {
        SkontoDiscountLabelTextFactory(
            resources = androidContext().resources,
            discountPercentageFormatter = get(),
        )
    }
    factory {
        SkontoInfoBannerTextFactory(
            resources = androidContext().resources,
            skontoDiscountPercentageFormatter = get(),
            skontoRemainingDaysFormatter = get()
        )
    }
    factory {
        SkontoSavedAmountTextFactory(
            resources = androidContext().resources,
            amountFormatter = get(),
        )
    }
    factory {
        AmountFormatter(
            amountFormatter = currencyFormatterWithoutSymbol(),
        )
    }
}
