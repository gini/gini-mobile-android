package net.gini.android.capture.di

import net.gini.android.capture.internal.storage.ClientConfigurationStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val clientConfigurationModule = module {
    single { ClientConfigurationStorage(context = androidContext()) }
}
