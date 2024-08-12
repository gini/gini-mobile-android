package net.gini.android.bank.api.models

data class Configuration(
    val clientID: String,
    val isUserJourneyAnalyticsEnabled: Boolean,
    val isSkontoEnabled: Boolean,
    val isReturnAssistantEnabled: Boolean,
    val mixpanelToken: String?,
    val amplitudeApiKey: String?,

    )