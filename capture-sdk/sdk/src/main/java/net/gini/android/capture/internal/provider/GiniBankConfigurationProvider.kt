package net.gini.android.capture.internal.provider

import net.gini.android.capture.internal.network.Configuration

class GiniBankConfigurationProvider {

    private var configuration: Configuration = Configuration(
        clientID = "",
        isUserJourneyAnalyticsEnabled = false,
        isSkontoEnabled = false,
        isReturnAssistantEnabled = false,
        amplitudeApiKey = "",
        isTransactionDocsEnabled = false,
        isInstantPaymentEnabled = false,
        isEInvoiceEnabled = false,
    )

    fun provide(): Configuration = configuration

    fun update(configuration: Configuration) {
        this.configuration = configuration
    }
}
