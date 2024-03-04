package net.gini.android.health.sdk.review.model

import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp

/**
 * A payment request used for starting the bank app. Only the id is sent, but it is associated with a bank.
 */
data class PaymentRequest(
    val id: String,
    val bankApp: PaymentProviderApp,
)