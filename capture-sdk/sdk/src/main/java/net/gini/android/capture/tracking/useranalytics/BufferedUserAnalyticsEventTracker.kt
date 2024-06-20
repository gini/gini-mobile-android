package net.gini.android.capture.tracking.useranalytics

import android.content.Context
import net.gini.android.capture.internal.provider.InstallationIdProvider
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty
import net.gini.android.capture.tracking.useranalytics.tracker.AmplitudeUserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.tracker.MixPanelUserAnalyticsEventTracker
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.LinkedList
import java.util.Queue

internal class BufferedUserAnalyticsEventTracker(
    val context: Context,
    private val installationIdProvider: InstallationIdProvider = InstallationIdProvider(context),
) : UserAnalyticsEventTracker {

    private val LOG = LoggerFactory.getLogger(BufferedUserAnalyticsEventTracker::class.java)

    private var eventTrackers: MutableSet<UserAnalyticsEventTracker> =
        Collections.synchronizedSet(mutableSetOf())

    private val eventSuperProperties: Queue<Set<UserAnalyticsEventSuperProperty>> = LinkedList()
    private val userProperties: Queue<Set<UserAnalyticsUserProperty>> = LinkedList()
    private val events: Queue<Pair<UserAnalyticsEvent, Set<UserAnalyticsEventProperty>>> =
        LinkedList()

    fun setPlatformTokens(vararg tokens: UserAnalytics.AnalyticsApiKey) {
        tokens.forEach { token ->
            when (token) {
                is MixPanelUserAnalyticsEventTracker.MixpanelAnalyticsApiKey -> {
                    eventTrackers.removeIf { tracker -> tracker is MixPanelUserAnalyticsEventTracker }
                    eventTrackers.add(
                        MixPanelUserAnalyticsEventTracker(
                            context = context,
                            apiKey = token,
                            installationIdProvider = installationIdProvider
                        )
                    )

                    LOG.debug("Mixpanel Initialized")
                }

                is AmplitudeUserAnalyticsEventTracker.AmplitudeAnalyticsApiKey -> {
                    eventTrackers.removeIf { tracker -> tracker is AmplitudeUserAnalyticsEventTracker }

                    eventTrackers.add(
                        AmplitudeUserAnalyticsEventTracker(
                            context = context,
                            apiKey = token,
                            installationIdProvider = installationIdProvider
                        )
                    )

                    LOG.debug("Amplitude Initialized")
                }

                else -> throw IllegalArgumentException("Unsupported token type: ${token.javaClass.simpleName}")
            }
        }
        trySendEvents()
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
        if (eventTrackers.isEmpty()) {
            LOG.debug("No trackers found. Skipping sending events")
            return
        }

        LOG.debug("${eventTrackers.size} Tracker(s) found. Sending events...")


        while (eventSuperProperties.isNotEmpty()) {
            eventSuperProperties.poll()?.let { superPropertySet ->
                everyTracker { it.setEventSuperProperty(superPropertySet) }

            }
        }
        while (userProperties.isNotEmpty()) {
            userProperties.poll()?.let { userPropertiesSet ->
                everyTracker { it.setUserProperty(userPropertiesSet) }
            }
        }
        while (events.isNotEmpty()) {
            events.poll()?.let { eventToPropertiesPair ->
                val (event, properties) = eventToPropertiesPair
                everyTracker { it.trackEvent(event, properties) }
            }
        }


        LOG.debug("Events sent")
    }

    private fun everyTracker(block: (UserAnalyticsEventTracker) -> Unit) {
        eventTrackers.forEach(block)
    }

}