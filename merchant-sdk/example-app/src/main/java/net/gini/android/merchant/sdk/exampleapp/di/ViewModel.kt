package net.gini.android.merchant.sdk.exampleapp.di

import net.gini.android.merchant.sdk.exampleapp.MainViewModel
import net.gini.android.merchant.sdk.exampleapp.orders.data.HardcodedOrdersLocalDataSource
import net.gini.android.merchant.sdk.exampleapp.orders.data.OrdersRepository
import net.gini.android.merchant.sdk.exampleapp.orders.ui.OrderDetailsViewModel
import net.gini.android.merchant.sdk.exampleapp.orders.ui.OrdersViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel() }
    viewModel { OrdersViewModel(get(), get()) }
    viewModel { OrderDetailsViewModel() }
    factory { OrdersRepository(get()) }
    factory { HardcodedOrdersLocalDataSource() }
}