package net.gini.android.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ErrorEvent @JvmOverloads constructor(
    @Json(name = "device_model") val deviceModel: String,
    @Json(name = "os_name") val osName: String,
    @Json(name = "os_version") val osVersion: String,
    @Json(name = "capture_sdk_version") val captureSdkVersion: String,
    @Json(name = "api_lib_version") val apiLibVersion: String,
    @Json(name = "description") val description: String,
    @Json(name = "document_id") val documentId: String? = null,
    @Json(name = "original_request_id") val originalRequestId: String? = null,
)
