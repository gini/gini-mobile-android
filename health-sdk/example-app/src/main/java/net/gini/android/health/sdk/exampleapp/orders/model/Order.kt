package net.gini.android.health.sdk.exampleapp.orders.model

import net.gini.android.health.sdk.review.model.PaymentDetails

data class Order(val iban: String, val recipient: String, val amount: String, val purpose: String)

fun Order.getPaymentDetails(): PaymentDetails = PaymentDetails(recipient, iban, amount, purpose)