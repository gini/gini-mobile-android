package net.gini.android.capture.internal.network

data class AmplitudeRootModel(
    val apiKey: String,
    val events: List<AmplitudeEventModel>,
)

data class AmplitudeEventModel(
    val userId: String,
    val deviceId: String,
    val eventType: String,
    val sessionId: String,
    val eventId: String,
    val time: Long,
    val platform: String,
    val osVersion: String,
    val deviceManufacturer: String,
    val deviceBrand: String,
    val deviceModel: String,
    val versionName: String,
    val osName: String,
    val carrier: String,
    val language: String,
    val eventProperties: Map<String, Any>? = null,
    val userProperties: Map<String, Any>? = null,
)
