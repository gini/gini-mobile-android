package net.gini.android.core.api.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentRequestBody(
    @Json(name = "sourceDocumentLocation") val sourceDocumentLocation: String? = null,
    @Json(name = "paymentProvider") val paymentProvider: String,
    @Json(name = "recipient") val recipient: String,
    @Json(name = "iban") val iban: String,
    @Json(name = "amount") val amount: String,
    @Json(name = "purpose") val purpose: String,
    @Json(name = "bic") val bic: String? = null,
)
