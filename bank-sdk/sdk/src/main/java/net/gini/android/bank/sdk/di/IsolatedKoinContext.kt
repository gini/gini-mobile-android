package net.gini.android.bank.sdk.di

import net.gini.android.bank.sdk.capture.skonto.skontoScreenModule
import org.koin.dsl.koinApplication

object IsolatedKoinContext {

    private val koinApp = koinApplication {
        modules(skontoScreenModule)
    }

    val koin = koinApp.koin
}

fun getGiniKoin() = IsolatedKoinContext.koin