package net.gini.android.bank.sdk.screenapiexample.di

import net.gini.android.bank.sdk.screenapiexample.pay.PayViewModelJava
import net.gini.android.bank.sdk.screenapiexample.pay.PayViewModelKotlin
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { PayViewModelJava(get()) }
    viewModel { PayViewModelKotlin(get()) }
}