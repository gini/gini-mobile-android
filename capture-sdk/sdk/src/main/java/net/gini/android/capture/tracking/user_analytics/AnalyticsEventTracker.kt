package net.gini.android.capture

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import net.gini.android.capture.tracking.user_analytics.AnalyticsEvent
import net.gini.android.capture.tracking.user_analytics.AnalyticsScreen
import org.json.JSONObject

interface AnalyticsEventTracker {
    fun trackEvent(eventName: AnalyticsEvent, screen: AnalyticsScreen)
}


object AnalyticsEventTrackerBuilder {
    fun createAnalyticsEventTracker(
        applicationContext: Context
    ): AnalyticsEventTracker {
        return createAnalyticsEventTracker(EventTrackerPlatform.MIXPANEL, applicationContext)
    }

    private fun createAnalyticsEventTracker(
        platform: EventTrackerPlatform,
        applicationContext: Context
    ): AnalyticsEventTracker {
        return when (platform) {
            EventTrackerPlatform.ALL -> AllAnalyticsEventTrackers(applicationContext)
            EventTrackerPlatform.MIXPANEL -> MixPanelAnalyticsEventTracker(applicationContext)
        }
    }
}

private class AllAnalyticsEventTrackers(applicationContext: Context) : AnalyticsEventTracker {

    private val mixpanelAPI: AnalyticsEventTracker

    init {
        mixpanelAPI = MixPanelAnalyticsEventTracker(applicationContext)
    }

    override fun trackEvent(eventName: AnalyticsEvent, screen: AnalyticsScreen) {
        mixpanelAPI.trackEvent(eventName, screen)
    }
}


private class MixPanelAnalyticsEventTracker(context: Context) : AnalyticsEventTracker {

    private val mixpanelAPI: MixpanelAPI

    init {
        mixpanelAPI = MixpanelAPI.getInstance(context, context.getString(R.string.mixpanel_api_key), false)
    }

    override fun trackEvent(eventName: AnalyticsEvent, screen: AnalyticsScreen) {
        val props = JSONObject()
        props.put("screen", screen.screenName)
        mixpanelAPI.track(eventName.eventName, props)
    }
}

enum class EventTrackerPlatform {
    ALL,
    MIXPANEL
}