package net.gini.android.merchant.sdk.paymentComponentBottomSheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp

class PaymentComponentBottomSheetViewModel private constructor(paymentComponent: PaymentComponent?) : ViewModel() {
    private val _paymentProviderApp = MutableStateFlow<PaymentProviderApp?>(null)
    val paymentProviderApp: StateFlow<PaymentProviderApp?> = _paymentProviderApp
    class Factory(private val paymentComponent: PaymentComponent?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaymentComponentBottomSheetViewModel(paymentComponent) as T
        }
    }
}