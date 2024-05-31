package net.gini.android.merchant.sdk.integratedFlow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent


internal class ContainerViewModel(private val paymentComponent: PaymentComponent?) : ViewModel() {
    private val _paymentComponent = MutableStateFlow<PaymentComponent?>(null)
    val paymentComponentFlow: StateFlow<PaymentComponent?> = _paymentComponent

    init {
        _paymentComponent.value = paymentComponent
    }

    class Factory(val paymentComponent: PaymentComponent?): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContainerViewModel(paymentComponent = paymentComponent) as T
        }
    }
}