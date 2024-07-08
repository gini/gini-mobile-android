package net.gini.android.merchant.sdk.exampleapp.di

import net.gini.android.merchant.sdk.exampleapp.MainViewModel
import net.gini.android.merchant.sdk.exampleapp.orders.data.HardcodedOrdersLocalDataSource
import net.gini.android.merchant.sdk.exampleapp.orders.data.InvoicesRepository
import net.gini.android.merchant.sdk.exampleapp.orders.data.OrdersLocalDataSource
import net.gini.android.merchant.sdk.exampleapp.orders.ui.OrdersViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel() }
    viewModel { OrdersViewModel(get(), get()) }
    factory { InvoicesRepository(get(), get(), get(), get()) }
    factory { OrdersLocalDataSource(get()) }
    factory { HardcodedOrdersLocalDataSource(get()) }
}