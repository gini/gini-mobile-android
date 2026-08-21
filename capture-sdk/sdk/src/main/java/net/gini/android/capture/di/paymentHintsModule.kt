package net.gini.android.capture.di

import net.gini.android.capture.paymentHints.GetAlreadyPaidHintEnabledUseCase
import net.gini.android.capture.paymentHints.GetCreditNoteHintEnabledUseCase
import net.gini.android.capture.paymentHints.GetPaymentDueHintEnabledUseCase
import net.gini.android.capture.paymentHints.GetPaymentScheduleHintEnabledUseCase
import org.koin.dsl.module

internal val paymentHintsModule = module {

    factory {
        GetAlreadyPaidHintEnabledUseCase(
            giniBankConfigurationProvider = get(),
        )
    }

    factory {
        GetPaymentDueHintEnabledUseCase(
            giniBankConfigurationProvider = get(),
        )
    }

    factory {
        GetPaymentScheduleHintEnabledUseCase(
            giniBankConfigurationProvider = get(),
        )
    }

    factory {
        GetCreditNoteHintEnabledUseCase(
            giniBankConfigurationProvider = get(),
        )
    }

}
