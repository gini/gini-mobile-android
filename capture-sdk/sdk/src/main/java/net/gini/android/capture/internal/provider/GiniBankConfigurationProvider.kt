package net.gini.android.capture.internal.provider

import net.gini.android.capture.internal.network.Configuration

class GiniBankConfigurationProvider {

    // Written from multiple threads (the DataStore observer on the main thread and the
    // configuration network callback via CompletableFuture.thenAcceptAsync on a background
    // thread) and read from others (e.g. QR-code detection). @Volatile guarantees readers
    // observe the latest reference instead of a stale one.
    @Volatile
    private var configuration: Configuration = Configuration(
        clientID = "",
        isUserJourneyAnalyticsEnabled = false,
        isSkontoEnabled = false,
        isReturnAssistantEnabled = false,
        amplitudeApiKey = "",
        isTransactionDocsEnabled = false,
        isQrCodeEducationEnabled = false,
        isInstantPaymentEnabled = false,
        isEInvoiceEnabled = false,
        isSavePhotosLocallyEnabled = false,
        isAlreadyPaidHintEnabled = false,
        isPaymentDueHintEnabled = false,
        isUnsupportedQRCodeWarningEnabled = false
    )

    fun provide(): Configuration = configuration

    fun update(configuration: Configuration) {
        this.configuration = configuration
    }
}
