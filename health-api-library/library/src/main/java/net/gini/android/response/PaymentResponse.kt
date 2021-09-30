package net.gini.android.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class PaymentResponse(
    @Json(name = "paidAt") val paidAt: String,
    @Json(name = "recipient") val recipient: String,
    @Json(name = "iban") val iban: String,
    @Json(name = "bic") val bic: String? = null,
    @Json(name = "amount") val amount: String,
    @Json(name = "purpose") val purpose: String,
)