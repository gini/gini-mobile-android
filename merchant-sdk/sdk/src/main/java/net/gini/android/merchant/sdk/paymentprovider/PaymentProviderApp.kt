package net.gini.android.merchant.sdk.paymentprovider

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.annotation.ColorInt
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.merchant.sdk.util.extensions.generateBitmapDrawableIcon
import org.slf4j.LoggerFactory

internal const val Scheme = "ginipay" // It has to match the scheme in query tag in manifest
private const val PaymentPath = "payment"
internal const val QueryUri = "$Scheme://$PaymentPath/id"

internal fun getPaymentProviderAppUri(requestId: String) = "$Scheme://$PaymentPath/$requestId"

internal fun PackageManager.getInstalledPaymentProviderApps(): List<InstalledPaymentProviderApp> =
    queryIntentActivities(getPaymentProviderAppQueryIntent(), 0)
        .map { InstalledPaymentProviderApp.fromResolveInfo(it, this) }


internal fun PackageManager.getPaymentProviderApps(
    paymentProviders: List<PaymentProvider>,
    context: Context
): List<PaymentProviderApp> =
    linkInstalledPaymentProviderAppsWithPaymentProviders(paymentProviders)
        .map { (installedApp, paymentProvider) ->
            PaymentProviderApp.fromPaymentProvider(paymentProvider, installedApp, context)
        }

internal fun PackageManager.linkInstalledPaymentProviderAppsWithPaymentProviders(paymentProviders: List<PaymentProvider>): List<Pair<InstalledPaymentProviderApp?, PaymentProvider>> {
    val installedPaymentProviderApps = getInstalledPaymentProviderApps()
    return paymentProviders
        .map { paymentProvider ->
            installedPaymentProviderApps
                .find { installedApp -> paymentProvider.packageName == installedApp.packageName }
                ?.let { installedApp -> installedApp to paymentProvider } ?: (null to paymentProvider)
        }
}

private fun getPaymentProviderAppQueryIntent() = Intent().apply {
    action = Intent.ACTION_VIEW
    data = Uri.parse(QueryUri)
}

data class PaymentProviderApp(
    val name: String,
    val icon: BitmapDrawable?,
    val colors: PaymentProviderAppColors,
    internal val paymentProvider: PaymentProvider,
    internal val installedPaymentProviderApp: InstalledPaymentProviderApp? = null,
) {

    fun getIntent(paymentRequestId: String): Intent? = installedPaymentProviderApp?.let {
        Intent(it.launchIntent).apply {
            data = Uri.parse(getPaymentProviderAppUri(paymentRequestId))
        }
    }

    fun isInstalled() = installedPaymentProviderApp != null

    fun hasPlayStoreUrl() = paymentProvider.playStoreUrl != null

    fun hasSamePaymentProviderId(
        paymentProviderApp: PaymentProviderApp
    ): Boolean =
        paymentProvider.id == paymentProviderApp.paymentProvider.id

    fun hasSamePaymentProviderId(id: String): Boolean = paymentProvider.id == id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PaymentProviderApp

        if (name != other.name) return false
        if (icon != other.icon) return false
        if (colors != other.colors) return false
        if (paymentProvider != other.paymentProvider) return false
        return installedPaymentProviderApp == other.installedPaymentProviderApp
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (icon?.hashCode() ?: 0)
        result = 31 * result + colors.hashCode()
        result = 31 * result + paymentProvider.hashCode()
        result = 31 * result + (installedPaymentProviderApp?.hashCode() ?: 0)
        return result
    }


    companion object {
        internal const val ICON_SIZE = 32f // in dp

        private val LOG = LoggerFactory.getLogger(PaymentProviderApp::class.java)

        internal fun fromPaymentProvider(
            paymentProvider: PaymentProvider,
            installedPaymentProviderApp: InstalledPaymentProviderApp?,
            context: Context,
        ): PaymentProviderApp {
            if (installedPaymentProviderApp != null) {
                if (paymentProvider.packageName != installedPaymentProviderApp.packageName) {
                    val errorMessage = """
                        The payment provider and the installed bank app have different package names:
                            - Payment provider:     ${paymentProvider.packageName}
                            - Installed bank app:   ${installedPaymentProviderApp.packageName}
                    """.trimIndent()
                    LOG.error(errorMessage)
                    throw IllegalArgumentException(errorMessage)
                }
            }
            return PaymentProviderApp(
                name = paymentProvider.name,
                icon = context.generateBitmapDrawableIcon(paymentProvider.icon, paymentProvider.icon.size),
                colors = PaymentProviderAppColors(
                    backgroundColor = Color.parseColor("#${paymentProvider.colors.backgroundColorRGBHex}"),
                    textColor = Color.parseColor("#${paymentProvider.colors.textColoRGBHex}")
                ),
                paymentProvider = paymentProvider,
                installedPaymentProviderApp = installedPaymentProviderApp
            )
        }
    }
}

data class PaymentProviderAppColors(
    @ColorInt val backgroundColor: Int,
    @ColorInt val textColor: Int
)

data class InstalledPaymentProviderApp(
    val packageName: String,
    val version: String,
    val launchIntent: Intent
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InstalledPaymentProviderApp

        if (packageName != other.packageName) return false
        if (version != other.version) return false
        if (launchIntent.action != other.launchIntent.action) return false
        return launchIntent.component == other.launchIntent.component
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + launchIntent.hashCode()
        return result
    }

    companion object {

        internal fun fromResolveInfo(
            resolveInfo: ResolveInfo,
            packageManager: PackageManager
        ): InstalledPaymentProviderApp {
            val packageName = resolveInfo.activityInfo.applicationInfo.packageName
            return InstalledPaymentProviderApp(
                packageName = packageName,
                version = packageManager.getPackageInfo(packageName, 0).versionName,
                launchIntent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    component = ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)
                }
            )
        }
    }
}
