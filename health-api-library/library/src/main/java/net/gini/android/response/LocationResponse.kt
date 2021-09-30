package net.gini.android.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class LocationResponse(
    @Json(name = "location") val location: String
)
