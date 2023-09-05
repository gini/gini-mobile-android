package net.gini.android.bank.screen

import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import javax.inject.Inject


/**
 * Main Entry of the application.
 */

@HiltAndroidApp
class ExampleApp : MultiDexApplication() {
    @set:Inject var giniCaptureDefaultNetworkService: GiniCaptureDefaultNetworkService? = null

    fun clearGiniCaptureNetworkInstances() {
        giniCaptureDefaultNetworkService?.cleanup()
        giniCaptureDefaultNetworkService = null
    }

}