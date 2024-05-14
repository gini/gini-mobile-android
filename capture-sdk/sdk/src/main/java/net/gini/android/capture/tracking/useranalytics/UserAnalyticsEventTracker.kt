package net.gini.android.capture.tracking.useranalytics

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import net.gini.android.capture.R
import org.json.JSONObject

interface UserAnalyticsEventTracker {
    fun trackEvent(eventName: UserAnalyticsEvent, screen: UserAnalyticsScreen)
    fun trackEventWithProperties(eventName: UserAnalyticsEvent, screen: UserAnalyticsScreen, additionalProperties: Map<String, String>)
}


object UserAnalyticsEventTrackerBuilder {

    private lateinit var eventTracker: UserAnalyticsEventTracker

    fun createAnalyticsEventTracker(
        applicationContext: Context
    ) {
        eventTracker = createAnalyticsEventTracker(EventTrackerPlatform.MIXPANEL, applicationContext)
    }


    fun getAnalyticsEventTracker(
    ) = eventTracker

    private fun createAnalyticsEventTracker(
        platform: EventTrackerPlatform,
        applicationContext: Context
    ): UserAnalyticsEventTracker {
        return when (platform) {
            EventTrackerPlatform.MIXPANEL -> MixPanelUserAnalyticsEventTracker(applicationContext)
        }
    }
}


private class MixPanelUserAnalyticsEventTracker(context: Context) : UserAnalyticsEventTracker {

    private val mixpanelAPI: MixpanelAPI

    init {
        mixpanelAPI = MixpanelAPI.getInstance(context, context.getString(R.string.mixpanel_api_key), false)
    }

    override fun trackEvent(eventName: UserAnalyticsEvent, screen: UserAnalyticsScreen) {
        val props = JSONObject()
        props.put("screen", screen.screenName)
        mixpanelAPI.track(eventName.eventName, props)
    }

    override fun trackEventWithProperties(
        eventName: UserAnalyticsEvent,
        screen: UserAnalyticsScreen,
        additionalProperties: Map<String, String>
    ) {
        val props = JSONObject()
        props.put("screen", screen.screenName)
        props.put(additionalProperties.keys.first(), additionalProperties.values.first())
        mixpanelAPI.track(eventName.eventName, props)
    }
}

enum class EventTrackerPlatform {
    MIXPANEL
}