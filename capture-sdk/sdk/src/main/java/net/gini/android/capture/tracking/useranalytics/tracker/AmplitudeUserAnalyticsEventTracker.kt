package net.gini.android.capture.tracking.useranalytics.tracker

import android.content.Context
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.amplitude.android.DefaultTrackingOptions
import com.amplitude.common.Logger
import com.amplitude.core.events.Identify
import net.gini.android.capture.R
import net.gini.android.capture.internal.provider.InstallationIdProvider
import net.gini.android.capture.tracking.useranalytics.BufferedUserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty
import org.slf4j.LoggerFactory

internal class AmplitudeUserAnalyticsEventTracker(
    context: Context,
    apiKey: AmplitudeAnalyticsApiKey,
    installationIdProvider: InstallationIdProvider = InstallationIdProvider(context)
) : UserAnalyticsEventTracker {

    private val LOG = LoggerFactory.getLogger(AmplitudeUserAnalyticsEventTracker::class.java)

    private val superProperties = mutableSetOf<UserAnalyticsEventSuperProperty>()

    private val amplitude: Amplitude = Amplitude(
        configuration = Configuration(
            apiKey.key,
            context = context.applicationContext,
            defaultTracking = DefaultTrackingOptions.ALL
        )
    ).also {
        it.setDeviceId(installationIdProvider.getInstallationId())
        it.logger.logMode = Logger.LogMode.DEBUG
    }

    override fun setUserProperty(userProperties: Set<UserAnalyticsUserProperty>) {
        val identify = Identify()
        userProperties.forEach { identify.set(it.getPair().first, it.getPair().second) }
        amplitude.identify(identify)
    }

    override fun setEventSuperProperty(property: UserAnalyticsEventSuperProperty) {
        setEventSuperProperty(setOf(property))
    }

    override fun setEventSuperProperty(property: Set<UserAnalyticsEventSuperProperty>) {
        superProperties.addAll(property)
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
        val superPropertiesMap = superProperties.associate { it.getPair() }
        val propertiesMap = properties.associate { it.getPair() }
        val finalProperties = superPropertiesMap.plus(propertiesMap)

        amplitude.track(
            eventType = eventName.eventName,
            eventProperties = finalProperties
        )

        LOG.debug("\nEvent: ${eventName.eventName}\n" +
                properties.joinToString("\n") { "  ${it.getPair().first}=${it.getPair().second}" })
    }

    data class AmplitudeAnalyticsApiKey(private val apiKey: String) :
        UserAnalytics.AnalyticsApiKey(apiKey)
}