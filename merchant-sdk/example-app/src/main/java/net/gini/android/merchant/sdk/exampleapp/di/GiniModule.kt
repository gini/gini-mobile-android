package net.gini.android.merchant.sdk.exampleapp.di

import net.gini.android.health.api.GiniHealthAPIBuilder
import net.gini.android.merchant.sdk.GiniMerchant
import org.koin.dsl.module

val giniModule = module {
    single {
        GiniHealthAPIBuilder(get(), getProperty("clientId"), getProperty("clientSecret"), "example.com")
            .build()
    }
    single { GiniMerchant(get(), getProperty("clientId"), getProperty("clientSecret"), "example.com") }
}