package net.gini.android.health.api.models

import net.gini.android.health.api.response.PaymentProviderResponse
import java.util.Locale

/**
 * A payment provider is a Gini partner which integrated Gini Pay Connect (via Gini Bank SDK) into their mobile apps.
 */
data class PaymentProvider(
    val id: String,
    val name: String,
    /**
     * Package name of the bank app that corresponds to this provider.
     */
    val packageName: String,
    /**
     * The minimal required app versions per platform.
     */
    val appVersion: String,
    /**
     * Colors to use when displaying the payment provider.
     */
    val colors: Colors,
    /**
     * The icon to use when displaying the payment provider.
     */
    val icon: ByteArray,
    /**
     * The URL to the Play Store of the payment provider app.
     */
    val playStoreUrl: String? = null,
    /**
     * If the payment provider supports Gini Pay Connect integration
     */
    val gpcSupportedPlatforms: List<String>,
    /**
     * If the payment provider supports PDF sharing
     */
    val openWithSupportedPlatforms: List<String>
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PaymentProvider

        if (id != other.id) return false
        if (name != other.name) return false
        if (packageName != other.packageName) return false
        if (appVersion != other.appVersion) return false
        if (colors != other.colors) return false
        if (!icon.contentEquals(other.icon)) return false
        return playStoreUrl == other.playStoreUrl
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + appVersion.hashCode()
        result = 31 * result + colors.hashCode()
        result = 31 * result + icon.contentHashCode()
        result = 31 * result + (playStoreUrl?.hashCode() ?: 0)
        return result
    }

    fun gpcSupported(): Boolean = gpcSupportedPlatforms.map { it.lowercase(Locale.getDefault()) }.contains(ANDROID_PLATFORM)

    /**
     * A payment provider's color scheme.
     */
    data class Colors(
        val backgroundColorRGBHex: String,
        val textColoRGBHex: String,
    )

    companion object {
        const val ANDROID_PLATFORM = "android"
    }
}

internal fun PaymentProviderResponse.toPaymentProvider(icon: ByteArray) = PaymentProvider(
    id = id,
    name = name,
    packageName = packageNameAndroid,
    appVersion = minAppVersion.android,
    colors = PaymentProvider.Colors(
        backgroundColorRGBHex = colors.background,
        textColoRGBHex = colors.text,
    ),
    icon = icon,
    playStoreUrl = playStoreUrl,
    gpcSupportedPlatforms = gpcSupportedPlatforms ?: listOf("android"),
    openWithSupportedPlatforms = openWithSupportedPlatforms ?: listOf("android")
)