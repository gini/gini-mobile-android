package net.gini.android.capture.tracking.useranalytics

import android.content.Context
import android.util.Log
import com.mixpanel.android.mpmetrics.MixpanelAPI
import net.gini.android.capture.internal.provider.InstallationIdProvider
import net.gini.android.capture.tracking.useranalytics.properties.AnalyticsKeyPairProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty
import org.slf4j.LoggerFactory
import java.util.LinkedList
import java.util.Queue


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

    fun initialize(
        applicationContext: Context
    ) {
        if (eventTracker != null) return

        eventTracker =
            BufferedUserAnalyticsEventTracker(applicationContext)
    }

    fun setPlatformTokens(mixpanelToken: String, amplitudeApiKey: String) {
        (eventTracker as? BufferedUserAnalyticsEventTracker)?.setPlatformTokens(
            mixpanelToken,
            amplitudeApiKey
        )
    }

    fun getAnalyticsEventTracker(
    ) = eventTracker ?: throw IllegalStateException(
        "You need to initialize analytics by calling `UserAnalytics.initialize(...)`"
    )

    fun cleanup() {
        eventTracker = null
    }


}

private class BufferedUserAnalyticsEventTracker(val context: Context) : UserAnalyticsEventTracker {

    private val LOG = LoggerFactory.getLogger(BufferedUserAnalyticsEventTracker::class.java)
    private var eventTracker: UserAnalyticsEventTracker? = null
    private val eventSuperProperties: Queue<Set<AnalyticsKeyPairProperty>> = LinkedList()
    private val userProperties: Queue<Set<AnalyticsKeyPairProperty>> = LinkedList()
    private val events: Queue<Pair<UserAnalyticsEvent, Set<UserAnalyticsEventProperty>>> =
        LinkedList()

    fun setPlatformTokens(mixpanelToken: String, amplitudeApiKey: String) {
        val platform: EventTrackerPlatform? =
            if (mixpanelToken.isNotEmpty() && amplitudeApiKey.isNotEmpty()) {
                EventTrackerPlatform.BOTH
            } else if (mixpanelToken.isNotEmpty()) {
                EventTrackerPlatform.MIXPANEL
            } else if (amplitudeApiKey.isNotEmpty()) {
                EventTrackerPlatform.AMPLITUDE
            } else {
                LOG.debug("No platform token provided")
                null
            }
        if (platform != null) {
            eventTracker = createAnalyticsEventTracker(
                platform,
                context,
                mixpanelToken,
                amplitudeApiKey
            )
            trySendEvents()
        }

    }


    override fun setEventSuperProperty(property: Set<UserAnalyticsEventSuperProperty>) {
        this.eventSuperProperties.add(property)
        trySendEvents()
    }

    override fun setEventSuperProperty(property: UserAnalyticsEventSuperProperty) {
        setEventSuperProperty(setOf(property))
    }

    override fun setUserProperty(userProperties: Set<UserAnalyticsUserProperty>) {
        this.userProperties.add(userProperties)
        trySendEvents()
    }

    override fun setUserProperty(userProperty: UserAnalyticsUserProperty) {
        setUserProperty(setOf(userProperty))
    }

    override fun trackEvent(
        eventName: UserAnalyticsEvent,
        properties: Set<UserAnalyticsEventProperty>
    ) {
        events.add(Pair(eventName, properties))
        trySendEvents()
    }

    override fun trackEvent(eventName: UserAnalyticsEvent) {
        trackEvent(eventName, emptySet())
    }

    private fun trySendEvents() {
        if (eventTracker == null) {
            return
        } else {
            while (eventSuperProperties.isNotEmpty()) {
                eventSuperProperties.poll()?.forEach {
                    eventTracker?.setEventSuperProperty(it as UserAnalyticsEventSuperProperty)
                }
            }
            while (userProperties.isNotEmpty()) {
                userProperties.poll()?.forEach {
                    eventTracker?.setUserProperty(it as UserAnalyticsUserProperty)
                }
            }
            while (events.isNotEmpty()) {
                val event = events.poll()
                if (event != null)
                    eventTracker?.trackEvent(event.first, event.second)
            }
        }
    }

    private fun createAnalyticsEventTracker(
        platform: EventTrackerPlatform,
        applicationContext: Context,
        mixpanelToken: String,
        amplitudeApiKey: String
    ): UserAnalyticsEventTracker {
        return when (platform) {
            EventTrackerPlatform.MIXPANEL -> MixPanelUserAnalyticsEventTracker(
                applicationContext,
                mixpanelToken
            )

            EventTrackerPlatform.AMPLITUDE -> TODO()
            EventTrackerPlatform.BOTH -> MultiUserAnalyticsEventTracker(
                applicationContext,
                mixpanelToken,
                amplitudeApiKey
            )

            else -> throw IllegalStateException("No platform token provided")
        }
    }

}

private class MultiUserAnalyticsEventTracker(
    context: Context,
    mixpanelToken: String,
    amplitudeApiKey: String
) : UserAnalyticsEventTracker {

    private val mixpanelEventTracker: UserAnalyticsEventTracker

    init {
        mixpanelEventTracker =
            MixPanelUserAnalyticsEventTracker(context, mixpanelToken)
    }

    override fun setUserProperty(userProperties: Set<UserAnalyticsUserProperty>) {
        mixpanelEventTracker.setUserProperty(userProperties)
    }

    override fun setEventSuperProperty(property: UserAnalyticsEventSuperProperty) {
        setEventSuperProperty(setOf(property))
    }

    override fun setEventSuperProperty(property: Set<UserAnalyticsEventSuperProperty>) {
        mixpanelEventTracker.setEventSuperProperty(property)
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
        mixpanelEventTracker.trackEvent(eventName, properties)
    }
}


private class MixPanelUserAnalyticsEventTracker(
    context: Context,
    mixpanelToken: String,
    installationIdProvider: InstallationIdProvider = InstallationIdProvider(context)
) : UserAnalyticsEventTracker {

    private val mixpanelAPI: MixpanelAPI

    init {
        mixpanelAPI =
            MixpanelAPI.getInstance(context, mixpanelToken, false)

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
    MIXPANEL,
    AMPLITUDE,
    BOTH,
    NONE
}
