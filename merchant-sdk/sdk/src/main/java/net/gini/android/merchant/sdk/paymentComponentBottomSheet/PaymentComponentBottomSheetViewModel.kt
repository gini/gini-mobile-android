package net.gini.android.merchant.sdk.paymentComponentBottomSheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.util.BackListener

class PaymentComponentBottomSheetViewModel private constructor(val paymentComponent: PaymentComponent?, val backListener: BackListener?, val documentId: String) : ViewModel() {
    private val _paymentProviderApp = MutableStateFlow<PaymentProviderApp?>(null)
    val paymentProviderApp: StateFlow<PaymentProviderApp?> = _paymentProviderApp
    class Factory(private val paymentComponent: PaymentComponent?, private val backListener: BackListener?, private val documentId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaymentComponentBottomSheetViewModel(paymentComponent, backListener, documentId) as T
        }
    }
}