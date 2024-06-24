package net.gini.android.capture.tracking.useranalytics

import android.content.Context

object UserAnalytics {

    private var eventTracker: UserAnalyticsEventTracker? = null

    fun initialize(
        applicationContext: Context
    ) {
        if (eventTracker != null) return

        eventTracker = BufferedUserAnalyticsEventTracker(applicationContext)
    }

    fun setPlatformTokens(vararg tokens: AnalyticsApiKey) {
        (eventTracker as? BufferedUserAnalyticsEventTracker)?.setPlatformTokens(*tokens)
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