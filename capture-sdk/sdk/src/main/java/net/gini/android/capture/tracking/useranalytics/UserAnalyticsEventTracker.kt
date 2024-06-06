package net.gini.android.capture.tracking.useranalytics

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Context.ACCESSIBILITY_SERVICE
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.mixpanel.android.mpmetrics.MixpanelAPI
import net.gini.android.capture.R
import net.gini.android.capture.internal.provider.InstallationIdProvider
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty


interface UserAnalyticsEventTracker {

    fun setEventSuperProperty(property: UserAnalyticsEventSuperProperty)

    fun setEventSuperProperty(property: Set<UserAnalyticsEventSuperProperty>)

    fun setUserProperty(userProperty: UserAnalyticsUserProperty)
    fun setUserProperty(userProperties: Set<UserAnalyticsUserProperty>)
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
        if (eventTracker != null) return

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


private class MixPanelUserAnalyticsEventTracker(
    context: Context,
    installationIdProvider: InstallationIdProvider = InstallationIdProvider(context)
) : UserAnalyticsEventTracker {

    private val mixpanelAPI: MixpanelAPI

    init {
        mixpanelAPI =
            MixpanelAPI.getInstance(context, context.getString(R.string.mixpanel_api_key), false)
        mixpanelAPI.setServerURL(MIXPANEL_SERVER_URL)

        mixpanelAPI.identify(installationIdProvider.getInstallationId())

        trackAccessibilityProperties(context)
    }

    private fun trackAccessibilityProperties(context: Context) = runCatching {
        val accessibilityManager =
            context.getSystemService(ACCESSIBILITY_SERVICE) as? AccessibilityManager

        val isBoldTextEnabled = context.resources.configuration.fontWeightAdjustment > 0

        val visualServiceList =
            accessibilityManager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                ?: emptyList()

        val isSpeakSelectionEnabled =
            visualServiceList.find { it.id == SELECT_TO_SPEAK_SERVICE_ID } != null
        val isSpeakScreenEnabled =
            visualServiceList.find { it.id == SPEAK_SCREEN_SERVICE_ID } != null

        val isGrayscaleEnabled =
            Settings.Secure.getInt(
                context.contentResolver, SETTING_NAME_DISPLAY_DALTONIZER, 0
            ) == 0 && Settings.Secure.getInt(
                context.contentResolver, SETTING_NAME_DISPLAY_DALTONIZER_ENABLED, 0
            ) == 1

        val properties = setOf(
            UserAnalyticsUserProperty.Accessibility.GrayscaleEnabled(isGrayscaleEnabled),
            UserAnalyticsUserProperty.Accessibility.BoldTextEnabled(isBoldTextEnabled),
            UserAnalyticsUserProperty.Accessibility.SpeakScreenEnabled(isSpeakScreenEnabled),
            UserAnalyticsUserProperty.Accessibility.SpeakSelectionEnabled(isSpeakSelectionEnabled),
        )

        setUserProperty(properties)
    }

    override fun setUserProperty(userProperties: Set<UserAnalyticsUserProperty>) {
        mixpanelAPI.people.setOnceMap(userProperties.associate { it.getPair() }.toMutableMap())
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

    companion object {
        private const val MIXPANEL_SERVER_URL = "https://api-eu.mixpanel.com"

        private const val SELECT_TO_SPEAK_SERVICE_ID =
            "com.google.android.marvin.talkback/com.google.android.accessibility.selecttospeak.SelectToSpeakService"
        private const val SPEAK_SCREEN_SERVICE_ID =
            "com.google.android.marvin.talkback/.TalkBackService"

        private const val SETTING_NAME_DISPLAY_DALTONIZER = "accessibility_display_daltonizer"
        private const val SETTING_NAME_DISPLAY_DALTONIZER_ENABLED =
            "accessibility_display_daltonizer_enabled"
    }
}

enum class EventTrackerPlatform {
    MIXPANEL
}
