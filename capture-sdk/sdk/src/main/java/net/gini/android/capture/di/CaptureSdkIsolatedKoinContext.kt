package net.gini.android.capture.di

import org.koin.dsl.koinApplication

object CaptureSdkIsolatedKoinContext {

    private val koinApp = koinApplication {
        modules(providerModule, analyticsModule)
    }

    val koin = koinApp.koin
}

fun getGiniCaptureKoin() = CaptureSdkIsolatedKoinContext.koin
