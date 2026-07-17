package net.gini.android.bank.sdk.capture.digitalinvoice

import net.gini.android.capture.network.model.GiniCaptureReturnReason
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

internal val returnReasonsScreenModule = module {
    viewModel { (reasons: List<GiniCaptureReturnReason>) ->
        ReturnReasonsViewModel(reasons = reasons)
    }
}
