package net.gini.android.health.sdk.exampleapp.di

import net.gini.android.health.sdk.exampleapp.MainViewModel
import net.gini.android.health.sdk.exampleapp.invoices.data.HardcodedInvoicesLocalDataSource
import net.gini.android.health.sdk.exampleapp.invoices.data.InvoicesLocalDataSource
import net.gini.android.health.sdk.exampleapp.invoices.data.InvoicesRepository
import net.gini.android.health.sdk.exampleapp.invoices.ui.InvoicesViewModel
import net.gini.android.health.sdk.exampleapp.orders.OrderDetailsViewModel
import net.gini.android.health.sdk.exampleapp.review.ReviewViewModel
import net.gini.android.health.sdk.exampleapp.upload.UploadViewModel
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(get()) }
    viewModel { UploadViewModel(get(), get(), get()) }
    viewModel { ReviewViewModel(get()) }
    viewModel { InvoicesViewModel(get(), get()) }
    viewModel { OrderDetailsViewModel() }
    factory { InvoicesRepository(get(), get(), get(), get()) }
    factory { InvoicesLocalDataSource(get()) }
    factory { HardcodedInvoicesLocalDataSource(get()) }
    factory { PaymentComponent(get(), get()) }
}