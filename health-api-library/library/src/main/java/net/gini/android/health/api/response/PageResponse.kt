package net.gini.android.health.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Created by Alpár Szotyori on 27.01.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

@JsonClass(generateAdapter = true)
internal data class PageResponse(
    @Json(name="pageNumber") val pageNumber: Int,
    @Json(name="images") val images: Map<String, String>
)