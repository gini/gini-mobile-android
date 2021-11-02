package net.gini.pay.appscreenapi.di

import net.gini.pay.appscreenapi.pay.PayViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { PayViewModel(get()) }
}