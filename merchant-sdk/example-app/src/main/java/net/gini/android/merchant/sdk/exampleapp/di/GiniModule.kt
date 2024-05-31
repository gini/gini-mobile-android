package net.gini.android.merchant.sdk.exampleapp.di

import net.gini.android.merchant.sdk.GiniHealth
import net.gini.android.merchant.sdk.util.getGiniApi
import org.koin.dsl.module

val giniModule = module {
    single { getGiniApi(get(), getProperty("clientId"), getProperty("clientSecret"), "example.com") }
    single { GiniHealth(get()) }
}