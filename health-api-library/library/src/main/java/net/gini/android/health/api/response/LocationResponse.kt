package net.gini.android.health.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class LocationResponse(
    @Json(name = "location") val location: String
)
