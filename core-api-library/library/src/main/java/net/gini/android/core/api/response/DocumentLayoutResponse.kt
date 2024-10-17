package net.gini.android.core.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DocumentLayoutResponse(
    val pages: List<PageResponse>
) {

    @JsonClass(generateAdapter = true)
    data class PageResponse(
        val number: Int,
        val sizeX: Float,
        val sizeY: Float,
        val textZones: List<TextZoneResponse>,
        val regions: List<RegionResponse>
    ) {

        @JsonClass(generateAdapter = true)
        data class TextZoneResponse(
            val paragraphs: List<ParagraphResponse>,
        ) {

            @JsonClass(generateAdapter = true)
            data class ParagraphResponse(
                @Json(name = "w") val width: Float,
                @Json(name = "h") val height: Float,
                @Json(name = "t") val top: Float,
                @Json(name = "l") val left: Float,
                val lines: List<LineResponse>,
            ) {

                @JsonClass(generateAdapter = true)
                data class LineResponse(
                    @Json(name = "w") val width: Float,
                    @Json(name = "h") val height: Float,
                    @Json(name = "t") val top: Float,
                    @Json(name = "l") val left: Float,
                    @Json(name = "wds") val words: List<WordResponse>,
                ) {

                    @JsonClass(generateAdapter = true)
                    data class WordResponse(
                        @Json(name = "w") val width: Float,
                        @Json(name = "h") val height: Float,
                        @Json(name = "t") val top: Float,
                        @Json(name = "l") val left: Float,
                        val fontSize: Float,
                        val fontFamily: String,
                        val bold: Boolean,
                        val text: String,
                    )
                }
            }
        }

        @JsonClass(generateAdapter = true)
        data class RegionResponse(
            @Json(name = "w") val width: Float,
            @Json(name = "h") val height: Float,
            @Json(name = "t") val top: Float,
            @Json(name = "l") val left: Float,
            val type: String,
        )
    }
}