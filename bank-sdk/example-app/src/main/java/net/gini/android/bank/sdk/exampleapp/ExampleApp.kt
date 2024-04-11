package net.gini.android.bank.sdk.exampleapp

import androidx.annotation.VisibleForTesting
import androidx.multidex.MultiDexApplication
import androidx.test.espresso.idling.CountingIdlingResource
import dagger.hilt.android.HiltAndroidApp


/**
 * Main Entry of the application.
 */

@HiltAndroidApp
class ExampleApp : MultiDexApplication() {

    val idlingResourceForOpenWith = CountingIdlingResource("OpenWith")
}