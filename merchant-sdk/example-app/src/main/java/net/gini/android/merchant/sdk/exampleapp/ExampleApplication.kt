package net.gini.android.merchant.sdk.exampleapp

import android.app.Application
import net.gini.android.merchant.sdk.exampleapp.di.giniModule
import net.gini.android.merchant.sdk.exampleapp.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.fileProperties

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