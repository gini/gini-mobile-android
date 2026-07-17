package net.gini.android.bank.sdk.capture.skonto.help

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

internal val skontoHelpScreenModule = module {
    viewModel {
        SkontoHelpViewModel()
    }
}
