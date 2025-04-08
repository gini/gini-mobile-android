package net.gini.android.bank.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.gini.android.bank.api.models.Configuration

@JsonClass(generateAdapter = true)
data class ConfigurationResponse(
    @Json(name = "clientID") val clientID: String?,
    @Json(name = "userJourneyAnalyticsEnabled") val userJourneyAnalyticsEnabled: Boolean?,
    @Json(name = "skontoEnabled") val skontoEnabled: Boolean?,
    @Json(name = "returnAssistantEnabled") val returnAssistantEnabled: Boolean?,
    @Json(name = "amplitudeApiKey") val amplitudeApiKey: String?,
    @Json(name = "transactionDocsEnabled") val transactionDocsEnabled: Boolean?,
    @Json(name = "instantPayment") val instantPaymentEnabled: Boolean?,
)

internal fun ConfigurationResponse.toConfiguration() = Configuration(
    clientID = clientID ?: "",
    isUserJourneyAnalyticsEnabled = userJourneyAnalyticsEnabled ?: false,
    isSkontoEnabled = skontoEnabled ?: false,
    isReturnAssistantEnabled = returnAssistantEnabled ?: false,
    amplitudeApiKey = amplitudeApiKey,
    transactionDocsEnabled = transactionDocsEnabled ?: false,
    instantPaymentEnabled = instantPaymentEnabled ?: false,
)

