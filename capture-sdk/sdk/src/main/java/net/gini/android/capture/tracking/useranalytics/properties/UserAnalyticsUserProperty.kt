package net.gini.android.capture.tracking.useranalytics.properties

import net.gini.android.capture.tracking.useranalytics.mapToAnalyticsValue

sealed class UserAnalyticsUserProperty(val propertyName: String, val value: String) {
    data class GiniClientId(val clientId: String) :
        UserAnalyticsUserProperty("gini_client_id", clientId)

    data class ReturnReasonsEnabled(val isEnabled: Boolean) :
        UserAnalyticsUserProperty("return_reasons_enabled", isEnabled.mapToAnalyticsValue())

    data class ReturnAssistantEnabled(val isEnabled: Boolean) :
        UserAnalyticsUserProperty("return_assistant_enabled", isEnabled.mapToAnalyticsValue())

    data class EntryPoint(val entryPointType: EntryPointType) :
        UserAnalyticsUserProperty("entry_point", entryPointType.analyticsName) {
        enum class EntryPointType(val analyticsName: String) {
            OPEN_WITH("open_with"), BUTTON("button"), FIELD("field")
        }
    }

    sealed class Accessibility {
        data class GrayscaleEnabled(private val isEnabled: Boolean) :
            UserAnalyticsUserProperty("is_grayscale_enabled", isEnabled.mapToAnalyticsValue())

        data class BoldTextEnabled(private val isEnabled: Boolean) :
            UserAnalyticsUserProperty("is_bold_text_enabled", isEnabled.mapToAnalyticsValue())

        data class SpeakSelectionEnabled(private val isEnabled: Boolean) :
            UserAnalyticsUserProperty("is_speak_selection_enabled", isEnabled.mapToAnalyticsValue())

        data class SpeakScreenEnabled(private val isEnabled: Boolean) :
            UserAnalyticsUserProperty("is_speak_screen_enabled", isEnabled.mapToAnalyticsValue())
    }

    fun getPair() = propertyName to value
}