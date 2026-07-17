package net.gini.android.bank.sdk.capture.digitalinvoice.details

import net.gini.android.bank.sdk.capture.digitalinvoice.SelectableLineItem
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

internal val lineItemDetailsScreenModule = module {
    viewModel { (selectableLineItem: SelectableLineItem) ->
        LineItemDetailsViewModel(
            selectableLineItem = selectableLineItem,
        )
    }
}
