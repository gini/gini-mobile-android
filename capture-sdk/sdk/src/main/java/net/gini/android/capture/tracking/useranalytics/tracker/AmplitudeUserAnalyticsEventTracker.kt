package net.gini.android.capture.tracking.useranalytics.tracker

import android.content.Context
import android.util.Log
import net.gini.android.capture.internal.network.AmplitudeEventModel
import net.gini.android.capture.internal.network.AmplitudeRoot
import net.gini.android.capture.internal.network.NetworkRequestsManager
import net.gini.android.capture.internal.provider.InstallationIdProvider
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty
import org.slf4j.LoggerFactory
import java.util.UUID

internal class AmplitudeUserAnalyticsEventTracker(
    val context: Context,
    val apiKey: AmplitudeAnalyticsApiKey,
    val networkRequestsManager: NetworkRequestsManager,
    val installationIdProvider: InstallationIdProvider = InstallationIdProvider(context)
) : UserAnalyticsEventTracker {

    private val LOG = LoggerFactory.getLogger(AmplitudeUserAnalyticsEventTracker::class.java)

    private val superProperties = mutableSetOf<UserAnalyticsEventSuperProperty>()
    private lateinit var userProperties: Map<String, Any>

    private val contextProvider: DeviceInfo = DeviceInfo(
        context,
        shouldTrackAdid = false
    )

    override fun setUserProperty(userProperties: Set<UserAnalyticsUserProperty>) {
        this.userProperties = userProperties.associate { it.getPair() }
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

        val events = listOf(
            AmplitudeEventModel(
                userId = "",
                deviceId = installationIdProvider.getInstallationId(),
                eventType = eventName.eventName,
                time = System.currentTimeMillis(),
                platform = contextProvider.osName,
                osVersion = contextProvider.osVersion,
                deviceManufacturer = contextProvider.manufacturer,
                deviceBrand = contextProvider.brand,
                deviceModel = contextProvider.model,
                versionName = contextProvider.versionName ?: "unknown",
                osName = contextProvider.osName,
                carrier = contextProvider.carrier ?: "unknown",
                language = contextProvider.language,
                appSetId = contextProvider.appSetId ?: "unknown",
                eventProperties = finalProperties,
                userProperties = userProperties,
                appVersion = "1.0"
            )
        )

        val reqBody = AmplitudeRoot(apiKey = apiKey.key, events)
        networkRequestsManager.sendEvents(reqBody, UUID.randomUUID())

        Log.e("User journey", "Event: ${eventName.eventName}\n" +
                properties.joinToString("\n") { "  ${it.getPair().first}=${it.getPair().second}" })

        LOG.debug("\nEvent: ${eventName.eventName}\n" +
                properties.joinToString("\n") { "  ${it.getPair().first}=${it.getPair().second}" })
    }

    data class AmplitudeAnalyticsApiKey(private val apiKey: String) :
        UserAnalytics.AnalyticsApiKey(apiKey)
}