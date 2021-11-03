package net.gini.pay.appscreenapi

import android.app.Application
import net.gini.pay.appscreenapi.di.giniModule
import net.gini.pay.appscreenapi.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ExampleApplication)
            modules(giniModule, viewModelModule)
        }
    }
}