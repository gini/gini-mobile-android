package net.gini.android.bank.sdk.capture.digitalinvoice

import net.gini.android.bank.sdk.capture.util.OncePerInstallEventStore
import net.gini.android.bank.sdk.capture.util.SimpleBusEventStore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

internal val digitalInvoiceScreenModule = module {
    viewModel { (args: DigitalInvoiceViewModelArgs) ->
        DigitalInvoiceViewModel(
            extractions = args.extractions,
            compoundExtractions = args.compoundExtractions,
            returnReasons = args.returnReasons,
            skontoData = args.skontoData,
            isInaccurateExtraction = args.isInaccurateExtraction,
            savedInstanceBundle = args.savedInstanceBundle,
            oncePerInstallEventStore = OncePerInstallEventStore(androidContext()),
            simpleBusEventStore = SimpleBusEventStore(androidContext()),
            getSkontoDefaultSelectionStateUseCase = get(),
            getSkontoEdgeCaseUseCase = get(),
            getSkontoAmountUseCase = get(),
            getSkontoSavedAmountUseCase = get(),
            skontoInfoBannerTextFactory = get(),
        )
    }
}
