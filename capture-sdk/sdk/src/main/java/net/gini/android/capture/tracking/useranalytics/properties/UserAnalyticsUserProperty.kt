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

    data class BankSdkVersionName(val versionName: String) :
        UserAnalyticsUserProperty("bank_sdk_version", versionName)

    data class CaptureSdkVersionName(val versionName: String) :
        UserAnalyticsUserProperty("capture_sdk_version", versionName)
}