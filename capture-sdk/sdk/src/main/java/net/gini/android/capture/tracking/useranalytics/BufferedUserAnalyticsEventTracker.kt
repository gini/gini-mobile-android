package net.gini.android.capture.tracking.useranalytics

import android.content.Context
import androidx.annotation.VisibleForTesting
import net.gini.android.capture.internal.network.NetworkRequestsManager
import net.gini.android.capture.internal.provider.UniqueIdProvider
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty
import net.gini.android.capture.tracking.useranalytics.tracker.AmplitudeUserAnalyticsEventTracker
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.LinkedList
import java.util.Queue

internal class BufferedUserAnalyticsEventTracker(
    val context: Context,
    val sessionId: String,
    private val uniqueIdProvider: UniqueIdProvider = UniqueIdProvider(context),
) : UserAnalyticsEventTracker {

    private val LOG = LoggerFactory.getLogger(BufferedUserAnalyticsEventTracker::class.java)

    private var eventTrackers: MutableSet<UserAnalyticsEventTracker> =
        Collections.synchronizedSet(mutableSetOf())

    private val eventSuperProperties: Queue<Set<UserAnalyticsEventSuperProperty>> = LinkedList()
    private val userProperties: Queue<Set<UserAnalyticsUserProperty>> = LinkedList()
    private val events: Queue<Pair<UserAnalyticsEvent, Set<UserAnalyticsEventProperty>>> =
        LinkedList()
    private var amplitude: AmplitudeUserAnalyticsEventTracker? = null
    private var mIsUserJourneyEnabled = false

    fun setPlatformTokens(
        vararg tokens: UserAnalytics.AnalyticsApiKey,
        networkRequestsManager: NetworkRequestsManager,
        isUserJourneyEnabled: Boolean = false
    ) {
        mIsUserJourneyEnabled = isUserJourneyEnabled
        if (!isUserJourneyEnabled)
            return
        tokens.forEach { token ->
            when (token) {

                is AmplitudeUserAnalyticsEventTracker.AmplitudeAnalyticsApiKey -> {
                    eventTrackers.removeIf { tracker -> tracker is AmplitudeUserAnalyticsEventTracker }

                    amplitude = AmplitudeUserAnalyticsEventTracker(
                        context = context,
                        apiKey = token,
                        sessionId = sessionId,
                        networkRequestsManager = networkRequestsManager,
                        uniqueIdProvider = uniqueIdProvider
                    )
                    amplitude?.startRepeatingJob()
                    amplitude?.let { eventTrackers.add(it) }

                    LOG.debug("Amplitude Initialized")
                }

                else -> throw IllegalArgumentException("Unsupported token type: ${token.javaClass.simpleName}")
            }
        }
        trySendEvents()
    }


    override fun setEventSuperProperty(property: Set<UserAnalyticsEventSuperProperty>): Boolean {
        if (!mIsUserJourneyEnabled)
            return false
        this.eventSuperProperties.add(property)
        return trySendEvents()
    }

    override fun setEventSuperProperty(property: UserAnalyticsEventSuperProperty): Boolean {
        return setEventSuperProperty(setOf(property))
    }

    override fun setUserProperty(userProperties: Set<UserAnalyticsUserProperty>): Boolean {
        if (!mIsUserJourneyEnabled)
            return false
        this.userProperties.add(userProperties)
        return trySendEvents()
    }

    override fun setUserProperty(userProperty: UserAnalyticsUserProperty): Boolean {
        return setUserProperty(setOf(userProperty))
    }

    override fun trackEvent(
        eventName: UserAnalyticsEvent,
        properties: Set<UserAnalyticsEventProperty>
    ): Boolean {
        if (!mIsUserJourneyEnabled)
            return false
        events.add(Pair(eventName, properties))
        return trySendEvents()
    }

    override fun trackEvent(eventName: UserAnalyticsEvent): Boolean {
        return trackEvent(eventName, emptySet())
    }

    override fun flushEvents(): Boolean {
        return amplitude?.let {
            amplitude?.flushEvents()
        } ?: false
    }

    private fun trySendEvents(): Boolean {
        if (!mIsUserJourneyEnabled || eventTrackers.isEmpty()) {
            if (eventTrackers.isEmpty())
                LOG.debug("No trackers found. Skipping sending events")
            return false
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
        return true
    }

    private fun everyTracker(block: (UserAnalyticsEventTracker) -> Unit) {
        eventTrackers.forEach(block)
    }

    @VisibleForTesting
    internal fun getTrackers(): Set<UserAnalyticsEventTracker> = eventTrackers.toSet()

}