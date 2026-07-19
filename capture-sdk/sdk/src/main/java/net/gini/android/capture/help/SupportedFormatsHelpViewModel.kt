package net.gini.android.capture.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.di.CaptureSdkJavaInterop
import net.gini.android.capture.einvoice.GetEInvoiceFeatureEnabledUseCase

/**
 * Internal use only.
 *
 * ViewModel for the supported formats help screen. Assembles the formats list
 * based on the [GiniCapture] configuration and on whether the screen is shown
 * for a QR code document.
 *
 * @suppress
 */
internal class SupportedFormatsHelpViewModel @JvmOverloads constructor(
    isQrCodeDocument: Boolean,
    getEInvoiceFeatureEnabledUseCase: GetEInvoiceFeatureEnabledUseCase =
        CaptureSdkJavaInterop.getEInvoiceFeatureEnabledUseCase()
) : ViewModel() {

    private val _uiState: MutableStateFlow<SupportedFormatsUiState>
    val uiState: StateFlow<SupportedFormatsUiState>

    init {
        val isEInvoiceEnabled = getEInvoiceFeatureEnabledUseCase.invoke()
        _uiState = MutableStateFlow(
            SupportedFormatsUiState(
                formatItems = SupportedFormatsAdapter.setUpItems(
                    isQrCodeDocument,
                    isEInvoiceEnabled
                ),
                isEInvoiceEnabled = isEInvoiceEnabled,
                isBottomNavigationBarEnabled = GiniCapture.hasInstance() &&
                        GiniCapture.getInstance().isBottomNavigationBarEnabled,
            )
        )
        uiState = _uiState.asStateFlow()
    }

    class Factory(private val isQrCodeDocument: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SupportedFormatsHelpViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return SupportedFormatsHelpViewModel(isQrCodeDocument) as T
        }
    }
}

/**
 * Internal use only.
 *
 * @suppress
 */
internal data class SupportedFormatsUiState(
    val formatItems: List<Enum<*>>,
    val isEInvoiceEnabled: Boolean,
    val isBottomNavigationBarEnabled: Boolean,
)
