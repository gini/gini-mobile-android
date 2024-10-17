package net.gini.android.health.sdk.exampleapp.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.PaymentComponentConfiguration

class ReviewViewModel(val giniHealth: GiniHealth) : ViewModel() {
    val giniPaymentModule = giniHealth.giniInternalPaymentModule
    val paymentProviderAppsFlow = giniPaymentModule.paymentComponent.paymentProviderAppsFlow

    fun setPaymentComponentConfig(paymentComponentConfiguration: PaymentComponentConfiguration) {
        giniPaymentModule.paymentComponent.paymentComponentConfiguration = paymentComponentConfiguration
    }

    fun loadPaymentProviderApps() {
        viewModelScope.launch {
            giniPaymentModule.paymentComponent.loadPaymentProviderApps()
        }
    }
}
