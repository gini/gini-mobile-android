package net.gini.android.internal.payment.utils

import net.gini.android.internal.payment.api.model.PaymentRequest


interface PaymentEventListener {
    fun onError(e: Exception)
    fun onLoading()
    fun onPaymentRequestCreated(
        paymentRequest: PaymentRequest,
        paymentProviderName: String
    )
}
