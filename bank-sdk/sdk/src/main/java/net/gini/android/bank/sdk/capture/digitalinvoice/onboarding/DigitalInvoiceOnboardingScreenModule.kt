package net.gini.android.bank.sdk.capture.digitalinvoice.onboarding

import net.gini.android.bank.sdk.capture.util.OncePerInstallEventStore
import net.gini.android.bank.sdk.capture.util.SimpleBusEventStore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

internal val digitalInvoiceOnboardingScreenModule = module {
    viewModel {
        DigitalInvoiceOnboardingViewModel(
            oncePerInstallEventStore = OncePerInstallEventStore(androidContext()),
            simpleBusEventStore = SimpleBusEventStore(androidContext()),
        )
    }
}
