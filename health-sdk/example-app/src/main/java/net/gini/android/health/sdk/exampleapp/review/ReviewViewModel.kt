package net.gini.android.health.sdk.exampleapp.review

import androidx.lifecycle.ViewModel
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.PaymentComponentConfiguration

class ReviewViewModel(val giniHealth: GiniHealth, val paymentComponent: PaymentComponent) : ViewModel() {
    fun setPaymentComponentConfig(paymentComponentConfiguration: PaymentComponentConfiguration) {
        paymentComponent.paymentComponentConfiguration = paymentComponentConfiguration
    }
}
