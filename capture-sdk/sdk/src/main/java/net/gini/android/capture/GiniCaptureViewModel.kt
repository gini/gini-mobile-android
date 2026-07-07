package net.gini.android.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.internal.provider.UnsupportedQrWarningSessionPin
import net.gini.android.capture.internal.storage.ClientConfigurationStorage

internal class GiniCaptureViewModel(
    private val clientConfigurationStorage: ClientConfigurationStorage,
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
    private val unsupportedQrWarningSessionPin: UnsupportedQrWarningSessionPin,
) : ViewModel() {

    init {
        // DataStore is the single source of truth for the configuration flags.
        //
        // isUnsupportedQRCodeWarningEnabled must stay constant for the whole session. Normally
        // this observer pins it to the first persisted value emitted by DataStore — not to a
        // synchronous in-memory default: on a relaunch a persisted `true` may not have been
        // loaded yet, and pinning to the cold default would incorrectly disable the warning for
        // the entire run. The pin is first-wins and shared via UnsupportedQrWarningSessionPin:
        // should an unsupported QR code be scanned before the first emission, the camera screen
        // pins the session value instead (see CameraFragmentExtension).
        //
        // clientID and amplitudeApiKey are not persisted, so they are preserved from whatever the
        // provider already holds (empty on first launch, real after the API responds).
        viewModelScope.launch {
            clientConfigurationStorage.getConfiguration()
                .filterNotNull()
                .collect { config ->
                    val pinnedQrCodeWarningEnabled = unsupportedQrWarningSessionPin.pinIfAbsent {
                        config.isUnsupportedQRCodeWarningEnabled
                    }
                    giniBankConfigurationProvider.update { current ->
                        config.copy(
                            clientID = current.clientID,
                            amplitudeApiKey = current.amplitudeApiKey,
                            isUnsupportedQRCodeWarningEnabled = pinnedQrCodeWarningEnabled
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        // This ViewModel's lifetime is the capture session: clearing it ends the session, so the
        // pinned warning type is released and a configuration change can apply next session.
        unsupportedQrWarningSessionPin.reset()
        super.onCleared()
    }
}
