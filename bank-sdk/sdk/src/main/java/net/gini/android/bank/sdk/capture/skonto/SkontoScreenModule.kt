package net.gini.android.bank.sdk.capture.skonto

import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val skontoScreenModule = module {
    viewModel { (data: SkontoData) ->
        SkontoFragmentViewModel(
            data = data,
            getSkontoAmountUseCase = get(),
            getSkontoDiscountPercentageUseCase = get(),
            getSkontoEdgeCaseUseCase = get(),
            getSkontoSavedAmountUseCase = get(),
            getSkontoRemainingDaysUseCase = get(),
            getSkontoDefaultSelectionStateUseCase = get(),
            skontoExtractionsHandler = get(),
            lastAnalyzedDocumentProvider = get()
        )
    }
}