package net.gini.android.health.sdk.review.installApp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.review.ReviewViewModel
import org.slf4j.LoggerFactory

internal class InstallAppViewModel(internal val paymentComponent: PaymentComponent?) : ViewModel() {

    private val _paymentProviderApp = MutableStateFlow<PaymentProviderApp?>(null)
    val paymentProviderApp: StateFlow<PaymentProviderApp?> = _paymentProviderApp

    init {
        viewModelScope.launch {
            paymentComponent?.selectedPaymentProviderAppFlow?.collect { selectedPaymentProviderAppState ->
                when (selectedPaymentProviderAppState) {
                    is SelectedPaymentProviderAppState.AppSelected -> {
                        _paymentProviderApp.value = selectedPaymentProviderAppState.paymentProviderApp
                    }

                    SelectedPaymentProviderAppState.NothingSelected -> {
                        LOG.error("No selected payment provider app")
                    }
                }
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InstallAppViewModel::class.java)
    }

    class Factory(private val paymentComponent: PaymentComponent?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InstallAppViewModel(paymentComponent) as T
        }
    }
}