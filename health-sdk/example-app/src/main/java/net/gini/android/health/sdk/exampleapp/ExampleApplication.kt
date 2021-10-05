package net.gini.android.health.sdk.exampleapp

import android.app.Application
import net.gini.android.health.sdk.exampleapp.di.giniModule
import net.gini.android.health.sdk.exampleapp.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ExampleApplication)
            fileProperties("/client.properties")
            modules(giniModule, viewModelModule)
        }
    }
}