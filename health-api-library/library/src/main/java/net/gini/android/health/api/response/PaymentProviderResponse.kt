package net.gini.android.health.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.gini.android.health.api.models.ANDROID_PLATFORM
import java.util.Locale

@JsonClass(generateAdapter = true)
internal data class PaymentProviderResponse(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "packageNameAndroid") val packageNameAndroid: String,
    @Json(name = "minAppVersion") val minAppVersion: AppVersionResponse,
    @Json(name = "colors") val colors: Colors,
    @Json(name = "iconLocation") val iconLocation: String,
    @Json(name = "playStoreUrlAndroid") val playStoreUrl: String?,
    @Json(name = "gpcSupportedPlatforms") val gpcSupportedPlatforms: List<String>?,
    @Json(name = "openWithSupportedPlatforms") val openWithSupportedPlatforms: List<String>?
) {

    fun isEnabled(): Boolean = (gpcSupportedPlatforms?.map { it.lowercase(Locale.getDefault()) }?.contains(ANDROID_PLATFORM) == true )
            || (openWithSupportedPlatforms?.map { it.lowercase(Locale.getDefault()) }?.contains(ANDROID_PLATFORM) == true)
}

@JsonClass(generateAdapter = true)
internal data class AppVersionResponse(
    @Json(name = "android") val android: String,
)

@JsonClass(generateAdapter = true)
internal data class Colors(
    @Json(name = "background") val background: String,
    @Json(name = "text") val text: String,
)