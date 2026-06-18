package net.gini.android.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.internal.storage.ClientConfigurationStorage

internal class GiniCaptureViewModel(
    private val clientConfigurationStorage: ClientConfigurationStorage,
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
) : ViewModel() {

    init {
        // Read the session snapshot synchronously from the in-memory cache — no IO hop, no race.
        // The cache is kept warm by a background coroutine in ClientConfigurationStorage that lives
        // for the singleton's lifetime. On first install the cache is cold but DataStore is also
        // empty, so false is the correct default in both cases.
        val sessionQrCodeWarningEnabled = clientConfigurationStorage.getSessionQrCodeWarningEnabled()

        // Apply immediately so the provider is correct before any QR scan can happen.
        giniBankConfigurationProvider.update(
            giniBankConfigurationProvider.provide().copy(
                isUnsupportedQRCodeWarningEnabled = sessionQrCodeWarningEnabled
            )
        )

        // DataStore is the single source of truth for all other boolean flags.
        // clientID and amplitudeApiKey are not persisted, so they are preserved from
        // whatever the provider already holds (empty on first launch, real after API responds).
        // isUnsupportedQRCodeWarningEnabled is pinned to the session snapshot for the entire session.
        viewModelScope.launch {
            clientConfigurationStorage.getConfiguration()
                .filterNotNull()
                .collect { config ->
                    giniBankConfigurationProvider.update(
                        config.copy(
                            clientID = giniBankConfigurationProvider.provide().clientID,
                            amplitudeApiKey = giniBankConfigurationProvider.provide().amplitudeApiKey,
                            isUnsupportedQRCodeWarningEnabled = sessionQrCodeWarningEnabled
                        )
                    )
                }
        }
    }
}
