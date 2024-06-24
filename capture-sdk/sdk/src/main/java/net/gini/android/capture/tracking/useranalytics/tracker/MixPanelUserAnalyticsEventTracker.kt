package net.gini.android.capture.tracking.useranalytics.tracker

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import net.gini.android.capture.internal.provider.InstallationIdProvider
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty
import org.slf4j.LoggerFactory

internal class MixPanelUserAnalyticsEventTracker(
    context: Context,
    apiKey: MixpanelAnalyticsApiKey,
    installationIdProvider: InstallationIdProvider = InstallationIdProvider(context)
) : UserAnalyticsEventTracker {

    private val LOG = LoggerFactory.getLogger(MixPanelUserAnalyticsEventTracker::class.java)

    private val mixpanelAPI: MixpanelAPI = MixpanelAPI
        .getInstance(context, apiKey.key, false)
        .also {
            it.identify(installationIdProvider.getInstallationId())
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
        LOG.debug("\nEvent: ${eventName.eventName}\n" +
                properties.joinToString("\n") { "  ${it.getPair().first}=${it.getPair().second}" })
    }

    data class MixpanelAnalyticsApiKey(private val apiKey: String) :
        UserAnalytics.AnalyticsApiKey(apiKey)
}