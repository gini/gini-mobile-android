package net.gini.pay.app.di

import net.gini.pay.app.MainViewModel
import net.gini.pay.app.review.ReviewViewModel
import net.gini.pay.app.upload.UploadViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(get()) }
    viewModel { UploadViewModel(get(), get()) }
    viewModel { ReviewViewModel(get()) }
}