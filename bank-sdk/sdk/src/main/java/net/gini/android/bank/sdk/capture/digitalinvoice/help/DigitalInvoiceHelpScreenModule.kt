package net.gini.android.bank.sdk.capture.digitalinvoice.help

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

internal val digitalInvoiceHelpScreenModule = module {
    viewModel {
        DigitalInvoiceHelpViewModel()
    }
}
