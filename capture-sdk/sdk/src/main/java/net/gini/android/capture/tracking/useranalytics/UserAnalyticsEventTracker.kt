package net.gini.android.capture.tracking.useranalytics

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import net.gini.android.capture.R

interface UserAnalyticsEventTracker {
    fun trackEvent(eventName: UserAnalyticsEvent, screen: UserAnalyticsScreen)

    fun trackEvent(
        eventName: UserAnalyticsEvent,
        screen: UserAnalyticsScreen,
        properties: Map<UserAnalyticsExtraProperties, Any>
    )
}


object UserAnalytics {

    private var eventTracker: UserAnalyticsEventTracker? = null

    fun initialize(applicationContext: Context) {
        eventTracker =
            createAnalyticsEventTracker(EventTrackerPlatform.MIXPANEL, applicationContext)
    }

    fun getAnalyticsEventTracker(
    ) = eventTracker ?: throw IllegalStateException(
        "You need to initialize analytics by calling `UserAnalytics.initialize(...)`"
    )

    fun cleanup() {
        eventTracker = null
    }

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
        mixpanelAPI =
            MixpanelAPI.getInstance(context, context.getString(R.string.mixpanel_api_key), false)
    }

    override fun trackEvent(eventName: UserAnalyticsEvent, screen: UserAnalyticsScreen) {
        trackEvent(eventName, screen, emptyMap())
    }

    override fun trackEvent(
        eventName: UserAnalyticsEvent,
        screen: UserAnalyticsScreen,
        properties: Map<UserAnalyticsExtraProperties, Any>
    ) {
        val defaultProperties = mapOf<String, Any>(
            UserAnalyticsExtraProperties.SCREEN.propertyName to screen.screenName
        )
        val finalProperties = defaultProperties.plus(properties.mapKeys { it.key.propertyName })
        mixpanelAPI.trackMap(eventName.eventName, finalProperties)
    }
}

enum class EventTrackerPlatform {
    MIXPANEL
}
