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
        // isUnsupportedQRCodeWarningEnabled must stay constant for the whole session, but it is
        // deliberately NOT pinned here. The first DataStore emission is the value cached by the
        // *previous* session; pinning it would latch that stale value and ignore the fresh remote
        // configuration saved later in this session — every session would run with the previous
        // session's flag. Instead the provider always tracks the latest persisted value, and the
        // camera screen pins it at the moment the first unsupported-QR warning is actually shown
        // (see CameraFragmentExtension.isUnsupportedQRCodeWarningEnabled).
        //
        // clientID and amplitudeApiKey are not persisted, so they are preserved from whatever the
        // provider already holds (empty on first launch, real after the API responds).
        viewModelScope.launch {
            clientConfigurationStorage.getConfiguration()
                .filterNotNull()
                .collect { config ->
                    giniBankConfigurationProvider.update { current ->
                        config.copy(
                            clientID = current.clientID,
                            amplitudeApiKey = current.amplitudeApiKey
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
