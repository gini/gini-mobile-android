package net.gini.android.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResolvePaymentResponse(
    @Json(name = "requesterUri") val requesterUri: String,
    @Json(name = "recipient") val recipient: String,
    @Json(name = "iban") val iban: String,
    @Json(name = "bic") val bic: String? = null,
    @Json(name = "amount") val amount: String,
    @Json(name = "purpose") val purpose: String,
    @Json(name = "status") val status: String,
)
