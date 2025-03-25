package net.gini.android.health.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConfigurationResponse(
    @Json(name = "clientID") val clientId: String,
    @Json(name = "communicationTone") val communicationTone: CommunicationTone?,
    @Json(name = "ingredientBrandType") val ingredientBrandType: String
)

enum class CommunicationTone {
    FORMAL,
    INFORMAL;
    companion object
}

fun CommunicationTone.Companion.getValue(visibility: String): CommunicationTone = try {
    CommunicationTone.valueOf(visibility)
} catch (exception: IllegalArgumentException) {
    CommunicationTone.FORMAL
}

enum class IngredientBrandType(visibility: String) {
    FULL_VISIBLE("FULL_VISIBLE"),
    PAYMENT_COMPONENT("PAYMENT_COMPONENT"),
    INVISIBLE("INVISIBLE");
    companion object
}

fun IngredientBrandType.Companion.getValue(visibility: String): IngredientBrandType = try {
    IngredientBrandType.valueOf(visibility)
} catch (exception: IllegalArgumentException) {
    IngredientBrandType.INVISIBLE
}