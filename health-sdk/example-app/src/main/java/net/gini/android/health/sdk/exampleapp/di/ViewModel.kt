package net.gini.android.health.sdk.exampleapp.di

import net.gini.android.health.sdk.exampleapp.MainViewModel
import net.gini.android.health.sdk.exampleapp.review.ReviewViewModel
import net.gini.android.health.sdk.exampleapp.upload.UploadViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(get()) }
    viewModel { UploadViewModel(get(), get()) }
    viewModel { ReviewViewModel(get()) }
}