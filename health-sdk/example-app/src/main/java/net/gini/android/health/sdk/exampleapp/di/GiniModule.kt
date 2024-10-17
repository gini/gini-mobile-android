package net.gini.android.health.sdk.exampleapp.di

import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.util.getGiniApi
import org.koin.dsl.module

val giniModule = module {
    single { getGiniApi(get(), getProperty("clientId"), getProperty("clientSecret"), "example.com") }
    single { GiniHealth(get(), get()) }
}