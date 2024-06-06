package net.gini.android.capture.tracking.useranalytics.properties

import net.gini.android.capture.tracking.useranalytics.mapToAnalyticsValue

sealed class UserAnalyticsUserProperty(key: String, value: String) :
    AnalyticsKeyPairProperty(key, value) {
    data class GiniClientId(val clientId: String) :
        UserAnalyticsUserProperty("gini_client_id", clientId)

    data class ReturnReasonsEnabled(val isEnabled: Boolean) :
        UserAnalyticsUserProperty("return_reasons_enabled", isEnabled.mapToAnalyticsValue())

    data class ReturnAssistantEnabled(val isEnabled: Boolean) :
        UserAnalyticsUserProperty("return_assistant_enabled", isEnabled.mapToAnalyticsValue())

    sealed class Accessibility {
        data class GrayscaleEnabled(private val isEnabled: Boolean) :
            UserAnalyticsUserProperty("grayscale_enabled", isEnabled.mapToAnalyticsValue())

        data class BoldTextEnabled(private val isEnabled: Boolean) :
            UserAnalyticsUserProperty("bold_text_enabled", isEnabled.mapToAnalyticsValue())

        data class SpeakSelectionEnabled(private val isEnabled: Boolean) :
            UserAnalyticsUserProperty("speak_selection_enabled", isEnabled.mapToAnalyticsValue())

        data class SpeakScreenEnabled(private val isEnabled: Boolean) :
            UserAnalyticsUserProperty("speak_screen_enabled", isEnabled.mapToAnalyticsValue())
    }
}