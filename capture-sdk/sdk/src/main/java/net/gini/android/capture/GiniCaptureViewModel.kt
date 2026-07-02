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
        // DataStore is the single source of truth for the configuration flags.
        //
        // isUnsupportedQRCodeWarningEnabled must stay constant for the whole session. It is pinned
        // to the FIRST persisted value observed from DataStore, not to a synchronous in-memory
        // default: on a relaunch a persisted `true` may not have been loaded yet, and pinning to
        // the cold default would incorrectly disable the warning for the entire run. Deriving the
        // snapshot from the first non-null emission guarantees it reflects the persisted state.
        //
        // clientID and amplitudeApiKey are not persisted, so they are preserved from whatever the
        // provider already holds (empty on first launch, real after the API responds).
        viewModelScope.launch {
            var sessionQrCodeWarningEnabled: Boolean? = null
            clientConfigurationStorage.getConfiguration()
                .filterNotNull()
                .collect { config ->
                    val pinnedQrCodeWarningEnabled = sessionQrCodeWarningEnabled
                        ?: config.isUnsupportedQRCodeWarningEnabled
                            .also { sessionQrCodeWarningEnabled = it }
                    giniBankConfigurationProvider.update(
                        config.copy(
                            clientID = giniBankConfigurationProvider.provide().clientID,
                            amplitudeApiKey = giniBankConfigurationProvider.provide().amplitudeApiKey,
                            isUnsupportedQRCodeWarningEnabled = pinnedQrCodeWarningEnabled
                        )
                    )
                }
        }
    }
}
