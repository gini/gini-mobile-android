package net.gini.android.bank.api.models

data class Configuration(
    val clientID: String,
    val isUserJourneyAnalyticsEnabled: Boolean,
    val isSkontoEnabled: Boolean,
    val isReturnAssistantEnabled: Boolean,
    val amplitudeApiKey: String?,
    val isTransactionDocsEnabled: Boolean,
    val isInstantPaymentEnabled: Boolean,
    val isEInvoiceEnabled: Boolean,
    val isQrCodeEducationEnabled: Boolean,
    val savePhotosLocallyEnabled: Boolean,
    val isAlreadyPaidHintEnabled: Boolean,
    val isPaymentDueHintEnabled: Boolean,
)
