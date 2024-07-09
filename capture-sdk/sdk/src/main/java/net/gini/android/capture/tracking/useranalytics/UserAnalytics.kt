package net.gini.android.capture.tracking.useranalytics

import android.content.Context
import net.gini.android.capture.internal.network.NetworkRequestsManager

object UserAnalytics {

    private var eventTracker: UserAnalyticsEventTracker? = null

    fun initialize(
        applicationContext: Context
    ) {
        if (eventTracker != null) return

        eventTracker = BufferedUserAnalyticsEventTracker(applicationContext)
    }

    fun setPlatformTokens(vararg tokens: AnalyticsApiKey, networkRequestsManager: NetworkRequestsManager) {
        (eventTracker as? BufferedUserAnalyticsEventTracker)?.setPlatformTokens(*tokens, networkRequestsManager = networkRequestsManager)
    }

    fun getAnalyticsEventTracker(
    ) = eventTracker ?: throw IllegalStateException(
        "You need to initialize analytics by calling `UserAnalytics.initialize(...)`"
    )

    fun cleanup() {
        eventTracker = null
    }

    open class AnalyticsApiKey(val key: String)
}