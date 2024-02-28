package net.gini.android.capture

import android.content.Context
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.amplitude.android.DefaultTrackingOptions
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

interface EventTracker {
    fun trackEvent(eventName: String, screen: String)
}


object EventTrackerBuilder {
    fun createEventTracker(
        applicationContext: Context
    ): EventTracker {
        return createEventTracker(EventTrackerPlatform.ALL, applicationContext)
    }

    private fun createEventTracker(
        platform: EventTrackerPlatform,
        applicationContext: Context
    ): EventTracker {
        return when (platform) {
            EventTrackerPlatform.ALL -> AllEventTrackers(applicationContext)
            EventTrackerPlatform.AMPLITUDE -> AmplitudeEventTracker(applicationContext)
            EventTrackerPlatform.MIXPANEL -> MixPanelEventTracker(applicationContext)
        }
    }
}

private class AllEventTrackers(applicationContext: Context) : EventTracker {

    private val amplitude: EventTracker
    private val mixpanelAPI: EventTracker

    init {
        amplitude = AmplitudeEventTracker(applicationContext)
        mixpanelAPI = MixPanelEventTracker(applicationContext)
    }

    override fun trackEvent(eventName: String, screen: String) {
        amplitude.trackEvent(eventName, screen)
        mixpanelAPI.trackEvent(eventName, screen)
    }
}

private class AmplitudeEventTracker(applicationContext: Context) : EventTracker {

    private val amplitude: Amplitude

    init {
        amplitude = Amplitude(
            Configuration(
                apiKey = applicationContext.getString(R.string.amplitude_api_key),
                context = applicationContext,
                defaultTracking = DefaultTrackingOptions.NONE,
            )
        )
    }

    override fun trackEvent(eventName: String, screen: String) {
        amplitude.track(eventName, mutableMapOf("screen" to screen))
    }
}

private class MixPanelEventTracker(context: Context) : EventTracker {

    private val mixpanelAPI: MixpanelAPI

    init {
        mixpanelAPI = MixpanelAPI.getInstance(context, context.getString(R.string.mixpanel_api_key), false)
    }

    override fun trackEvent(eventName: String, screen: String) {
        val props = JSONObject()
        props.put("screen", screen)
        mixpanelAPI.track(eventName, props)
    }
}

enum class EventTrackerPlatform {
    ALL,
    AMPLITUDE,
    MIXPANEL
}