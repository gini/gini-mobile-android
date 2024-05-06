package net.gini.android.capture

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

interface EventTracker {
    fun trackEvent(eventName: String, screen: String)
}


object EventTrackerBuilder {
    fun createEventTracker(
        applicationContext: Context
    ): EventTracker {
        return createEventTracker(EventTrackerPlatform.ALL, applicationContext)
    }

    private fun createEventTracker(
        platform: EventTrackerPlatform,
        applicationContext: Context
    ): EventTracker {
        return when (platform) {
            EventTrackerPlatform.ALL -> AllEventTrackers(applicationContext)
            EventTrackerPlatform.MIXPANEL -> MixPanelEventTracker(applicationContext)
        }
    }
}

private class AllEventTrackers(applicationContext: Context) : EventTracker {

    private val mixpanelAPI: EventTracker

    init {
        mixpanelAPI = MixPanelEventTracker(applicationContext)
    }

    override fun trackEvent(eventName: String, screen: String) {
        mixpanelAPI.trackEvent(eventName, screen)
    }
}


private class MixPanelEventTracker(context: Context) : EventTracker {

    private val mixpanelAPI: MixpanelAPI

    init {
        mixpanelAPI = MixpanelAPI.getInstance(context, context.getString(R.string.mixpanel_api_key), false)
    }

    override fun trackEvent(eventName: String, screen: String) {
        val props = JSONObject()
        props.put("screen", screen)
        mixpanelAPI.track(eventName, props)
    }
}

enum class EventTrackerPlatform {
    ALL,
    MIXPANEL
}