package net.gini.android.capture.tracking.useranalytics.tracker

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.gini.android.capture.internal.network.AmplitudeEventModel
import net.gini.android.capture.internal.network.AmplitudeRoot
import net.gini.android.capture.internal.network.NetworkRequestsManager
import net.gini.android.capture.internal.provider.UniqueIdProvider
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty
import org.slf4j.LoggerFactory
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

internal class AmplitudeUserAnalyticsEventTracker(
    val context: Context,
    val apiKey: AmplitudeAnalyticsApiKey,
    val networkRequestsManager: NetworkRequestsManager,
    val uniqueIdProvider: UniqueIdProvider = UniqueIdProvider(context)
) : UserAnalyticsEventTracker {

    private val LOG = LoggerFactory.getLogger(AmplitudeUserAnalyticsEventTracker::class.java)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_INSTALLATION_ID = "installation_id"
    }

    private val superProperties = mutableSetOf<UserAnalyticsEventSuperProperty>()
    private lateinit var userProperties: Map<String, Any>

    private val contextProvider: DeviceInfo = DeviceInfo(
        context
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

    private val events: MutableList<AmplitudeEventModel> = mutableListOf()

    override fun trackEvent(
        eventName: UserAnalyticsEvent,
        properties: Set<UserAnalyticsEventProperty>
    ) {
        val superPropertiesMap = superProperties.associate { it.getPair() }
        val propertiesMap = properties.associate { it.getPair() }
        val finalProperties = superPropertiesMap.plus(propertiesMap)
        val c: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))

        events.add(
            AmplitudeEventModel(
                userId = uniqueIdProvider.getUniqueId(KEY_USER_ID),
                deviceId = uniqueIdProvider.getUniqueId(KEY_INSTALLATION_ID),
                eventType = eventName.eventName,
                time = c.timeInMillis,
                platform = contextProvider.osName,
                osVersion = contextProvider.osVersion,
                deviceManufacturer = contextProvider.manufacturer,
                deviceBrand = contextProvider.brand,
                deviceModel = contextProvider.model,
                versionName = contextProvider.versionName ?: "unknown",
                osName = contextProvider.osName,
                carrier = contextProvider.carrier ?: "unknown",
                language = contextProvider.language,
                eventProperties = finalProperties,
                userProperties = userProperties,
                appVersion = "1.0"
            )
        )

        Log.e("User journey", "Event: ${eventName.eventName}\n" +
                properties.joinToString("\n") { "  ${it.getPair().first}=${it.getPair().second}" })

        LOG.debug("\nEvent: ${eventName.eventName}\n" +
                properties.joinToString("\n") { "  ${it.getPair().first}=${it.getPair().second}" })
    }

    fun startRepeatingJob(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                sendEventsToAmplitudeApi()
                delay(5000)
            }
        }
    }

    override fun flushEvents() {
        CoroutineScope(Dispatchers.IO).launch {
            sendEventsToAmplitudeApi()
        }
    }

    private fun sendEventsToAmplitudeApi() {
        if (events.isNotEmpty()) {
            val reqBody = AmplitudeRoot(apiKey = apiKey.key, events.toList())
            networkRequestsManager.sendEvents(reqBody, UUID.randomUUID())
            events.clear()
        }
    }


    data class AmplitudeAnalyticsApiKey(private val apiKey: String) :
        UserAnalytics.AnalyticsApiKey(apiKey)
}