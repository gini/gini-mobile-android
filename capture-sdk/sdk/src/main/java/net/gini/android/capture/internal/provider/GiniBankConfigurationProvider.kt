package net.gini.android.capture.internal.provider

import net.gini.android.capture.internal.network.Configuration
import java.util.concurrent.atomic.AtomicReference

class GiniBankConfigurationProvider {

    // Updated from multiple threads (the DataStore observer on the main thread and the
    // configuration network callback via CompletableFuture.thenAcceptAsync on a background
    // thread) and read from others (e.g. QR-code detection). Updates go through a CAS loop so
    // each read-modify-write is atomic and concurrent updaters cannot overwrite each other's
    // fields (e.g. clientID reverting to "").
    private val configuration: AtomicReference<Configuration> = AtomicReference(
        Configuration(
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
    )

    fun provide(): Configuration = configuration.get()

    /**
     * Atomically replaces the configuration with the result of [transform] applied to the
     * current value. [transform] must be pure — it may be re-invoked when another thread
     * updates the configuration concurrently.
     */
    fun update(transform: (Configuration) -> Configuration) {
        while (true) {
            val current = configuration.get()
            if (configuration.compareAndSet(current, transform(current))) {
                return
            }
        }
    }
}
