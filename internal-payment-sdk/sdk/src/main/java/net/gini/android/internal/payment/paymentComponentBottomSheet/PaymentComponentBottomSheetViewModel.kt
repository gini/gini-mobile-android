package net.gini.android.internal.payment.paymentComponentBottomSheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentprovider.PaymentProviderApp
import net.gini.android.internal.payment.util.BackListener

internal class PaymentComponentBottomSheetViewModel private constructor(
    val paymentComponent: PaymentComponent?,
    val backListener: BackListener?,
    val reviewFragmentShown: Boolean) : ViewModel() {

    private val _paymentProviderApp = MutableStateFlow<PaymentProviderApp?>(null)
    val paymentProviderApp: StateFlow<PaymentProviderApp?> = _paymentProviderApp

    init {
        viewModelScope.launch {
            paymentComponent?.selectedPaymentProviderAppFlow?.collect {
                if (it is SelectedPaymentProviderAppState.AppSelected) {
                    _paymentProviderApp.value = it.paymentProviderApp
                }
            }
        }
    }

    class Factory(private val paymentComponent: PaymentComponent?,
                  private val backListener: BackListener?,
                  private val reviewFragmentShown: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaymentComponentBottomSheetViewModel(paymentComponent, backListener, reviewFragmentShown) as T
        }
    }
}
