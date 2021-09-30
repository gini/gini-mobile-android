package net.gini.android.models

import net.gini.android.response.PaymentResponse

data class Payment(
    val paidAt: String,
    val recipient: String,
    val iban: String,
    val amount: String,
    val purpose: String,
    val bic: String? = null,
)

internal fun PaymentResponse.toPayment() = Payment(
    paidAt, recipient, iban, amount, purpose, bic
)