package net.gini.android.capture.tracking.useranalytics

import android.content.Context
import net.gini.android.capture.internal.network.NetworkRequestsManager

object UserAnalytics {

    private var eventTracker: UserAnalyticsEventTracker? = null
    private lateinit var sessionId : String

    fun initialize(
        applicationContext: Context
    ) {
        if (eventTracker != null) return

        sessionId = System.currentTimeMillis().toString()
        eventTracker = BufferedUserAnalyticsEventTracker(applicationContext, sessionId)
    }

    fun setPlatformTokens(vararg tokens: AnalyticsApiKey, networkRequestsManager: NetworkRequestsManager) {
        (eventTracker as? BufferedUserAnalyticsEventTracker)?.setPlatformTokens(*tokens, networkRequestsManager = networkRequestsManager)
    }

    fun flushEvents() {
        (eventTracker as? BufferedUserAnalyticsEventTracker)?.flushEvents()
    }

    fun getAnalyticsEventTracker(
    ) = eventTracker

    fun cleanup() {
        eventTracker = null
    }

    open class AnalyticsApiKey(val key: String)
}