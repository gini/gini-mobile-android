package net.gini.pay.appscreenapi.di

import net.gini.android.core.api.DocumentMetadata
import net.gini.pay.appscreenapi.R
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.util.getGiniApi
import net.gini.android.bank.sdk.network.getDefaultNetworkApi
import net.gini.android.bank.sdk.network.getDefaultNetworkService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val giniModule = module {
    single {
        DocumentMetadata().apply {
            setBranchId("GiniBankExampleAndroid")
            add("AppFlow", "ScreenAPI")
        }
    }
    single { getDefaultNetworkService(get(), androidContext().getString(R.string.gini_api_client_id),
        androidContext().getString(R.string.gini_api_client_secret), "example.com", get()) }
    single { getDefaultNetworkApi(get()) }
    single { getGiniApi(get(), androidContext().getString(R.string.gini_api_client_id),
        androidContext().getString(R.string.gini_api_client_secret), "example.com") }
    single { GiniBank.apply { setGiniApi(get()) } }
}