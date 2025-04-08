package net.gini.android.bank.api.models

data class Configuration(
    val clientID: String,
    val isUserJourneyAnalyticsEnabled: Boolean,
    val isSkontoEnabled: Boolean,
    val isReturnAssistantEnabled: Boolean,
    val amplitudeApiKey: String?,
    val transactionDocsEnabled: Boolean,
    val instantPaymentEnabled: Boolean,
)