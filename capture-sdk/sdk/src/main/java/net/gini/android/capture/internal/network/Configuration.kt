package net.gini.android.capture.internal.network

import java.util.UUID

data class Configuration(
    val id: UUID = UUID.randomUUID(),
    val clientID: String,
    val isUserJourneyAnalyticsEnabled: Boolean,
    val isSkontoEnabled: Boolean,
    val isReturnAssistantEnabled: Boolean,
    val isTransactionDocsEnabled: Boolean,
    val isQrCodeEducationEnabled: Boolean,
    val isInstantPaymentEnabled: Boolean,
    val isEInvoiceEnabled: Boolean,
    val amplitudeApiKey: String
)
