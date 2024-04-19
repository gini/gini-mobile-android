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

    @VisibleForTesting
    internal val idlingResourceForOpenWith = CountingIdlingResource("OpenWith")

    fun incrementIdlingResourceForOpenWith() {
        idlingResourceForOpenWith.increment()
    }

    fun decrementIdlingResourceForOpenWith() {
        try {
            idlingResourceForOpenWith.decrement()
        } catch (e: IllegalStateException) {
            // ignore
        }
    }
}