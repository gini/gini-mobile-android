package net.gini.android.bank.sdk.screenapiexample.di

import net.gini.android.bank.sdk.screenapiexample.pay.PayViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { PayViewModel(get()) }
}