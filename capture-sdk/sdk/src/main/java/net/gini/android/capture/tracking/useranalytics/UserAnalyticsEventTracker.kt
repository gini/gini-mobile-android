package net.gini.android.capture.tracking.useranalytics

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import net.gini.android.capture.R
import net.gini.android.capture.internal.provider.InstallationIdProvider
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty


interface UserAnalyticsEventTracker {

    fun setEventSuperProperty(property: UserAnalyticsEventSuperProperty)

    fun setEventSuperProperty(property: Set<UserAnalyticsEventSuperProperty>)

    fun setUserProperty(userProperty: UserAnalyticsUserProperty)
    fun setUserProperty(userProperties: Set<UserAnalyticsUserProperty>)
    fun trackEvent(eventName: UserAnalyticsEvent)
    fun trackEvent(eventName: UserAnalyticsEvent, properties: Set<UserAnalyticsEventProperty>)
}


object UserAnalytics {

    private var eventTracker: UserAnalyticsEventTracker? = null

    fun initialize(applicationContext: Context) {
        if (eventTracker != null) return

        eventTracker =
            createAnalyticsEventTracker(EventTrackerPlatform.MIXPANEL, applicationContext)

        eventTracker?.trackEvent(UserAnalyticsEvent.SDK_OPENED)
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


private class MixPanelUserAnalyticsEventTracker(
    context: Context,
    installationIdProvider: InstallationIdProvider = InstallationIdProvider(context)
) : UserAnalyticsEventTracker {

    private val mixpanelAPI: MixpanelAPI

    init {
        mixpanelAPI =
            MixpanelAPI.getInstance(context, context.getString(R.string.mixpanel_api_key), false)

        mixpanelAPI.identify(installationIdProvider.getInstallationId())
    }

    override fun setUserProperty(userProperties: Set<UserAnalyticsUserProperty>) {
        mixpanelAPI.people.setMap(userProperties.associate { it.getPair() }.toMutableMap())
    }

    override fun setEventSuperProperty(property: UserAnalyticsEventSuperProperty) {
        setEventSuperProperty(setOf(property))
    }

    override fun setEventSuperProperty(property: Set<UserAnalyticsEventSuperProperty>) {
        mixpanelAPI.registerSuperPropertiesMap(property.associate { it.getPair() })
    }

    override fun setUserProperty(userProperty: UserAnalyticsUserProperty) {
        setUserProperty(setOf(userProperty))
    }

    override fun trackEvent(eventName: UserAnalyticsEvent) {
        trackEvent(eventName, emptySet())
    }

    override fun trackEvent(
        eventName: UserAnalyticsEvent,
        properties: Set<UserAnalyticsEventProperty>
    ) {
        mixpanelAPI.trackMap(eventName.eventName, properties.associate { it.getPair() })
    }
}

enum class EventTrackerPlatform {
    MIXPANEL
}
