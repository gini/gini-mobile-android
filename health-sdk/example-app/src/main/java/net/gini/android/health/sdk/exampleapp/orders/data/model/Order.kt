package net.gini.android.health.sdk.exampleapp.orders.data.model

import com.squareup.moshi.JsonClass
import net.gini.android.health.sdk.review.model.PaymentDetails

@JsonClass(generateAdapter = true)
data class Order(var id: String, var iban: String, var recipient: String, var amount: String, var purpose: String, var expiryDate: String? = null)

fun Order.getPaymentDetails(): PaymentDetails = PaymentDetails(recipient, iban, amount, purpose)