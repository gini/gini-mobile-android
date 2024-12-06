package net.gini.android.bank.api.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.gini.android.bank.api.models.AmplitudeEvent
import net.gini.android.bank.api.models.AmplitudeRoot

@JsonClass(generateAdapter = true)
data class AmplitudeRequestBody(
    @Json(name = "events") val events: List<AmplitudeEventBody>,
)

@JsonClass(generateAdapter = true)
data class AmplitudeEventBody(
    @Json(name = "user_id") val userId: String,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "event_type") val eventType: String,
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "event_id") val eventId: String,
    @Json(name = "time") val time: Long = 0,
    @Json(name = "platform") val platform: String,
    @Json(name = "os_version") val osVersion: String,
    @Json(name = "device_manufacturer") val deviceManufacturer: String,
    @Json(name = "device_brand") val deviceBrand: String,
    @Json(name = "device_model") val deviceModel: String,
    @Json(name = "version_name") val versionName: String,
    @Json(name = "os_name") val osName: String,
    @Json(name = "carrier") val carrier: String,
    @Json(name = "language") val language: String,
    @Json(name = "ip") val ip: String = "\$remote",
    @Json(name = "event_properties") val eventProperties: Map<String, Any>? = null,
    @Json(name = "user_properties") val userProperties: Map<String, Any>? = null,
)


internal fun AmplitudeRoot.toAmplitudeRequestBody() = AmplitudeRequestBody(
    events = events.map { it.toAmplitudeEventBody() }
)

internal fun AmplitudeEvent.toAmplitudeEventBody() = AmplitudeEventBody(
    userId = userId,
    deviceId = deviceId,
    eventType = eventType,
    sessionId = sessionId,
    eventId = eventId,
    time = time,
    platform = platform,
    osVersion = osVersion,
    deviceManufacturer = deviceManufacturer,
    deviceBrand = deviceBrand,
    deviceModel = deviceModel,
    versionName = versionName,
    osName = osName,
    carrier = carrier,
    language = language,
    eventProperties = eventProperties,
    userProperties = userProperties,
)
