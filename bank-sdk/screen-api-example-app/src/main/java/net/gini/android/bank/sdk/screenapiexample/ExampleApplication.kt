package net.gini.android.bank.sdk.screenapiexample

import android.app.Application
import net.gini.android.bank.sdk.screenapiexample.di.giniModule
import net.gini.android.bank.sdk.screenapiexample.di.viewModelModule
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