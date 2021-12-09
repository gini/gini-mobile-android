package net.gini.android.health.sdk.review.bank

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorInt
import net.gini.android.core.api.models.PaymentProvider

internal const val Scheme = "ginipay" // It has to match the scheme in query tag in manifest
private const val PaymentPath = "payment"
internal const val QueryUri = "$Scheme://$PaymentPath/id"
internal fun getBankUri(requestId: String) = "$Scheme://$PaymentPath/$requestId"

internal fun PackageManager.getInstalledBankApps(): List<InstalledBankApp> = queryIntentActivities(getBankQueryIntent(), 0)
    .map { InstalledBankApp.fromResolveInfo(it, this) }


internal fun PackageManager.getValidBankApps(paymentProviders: List<PaymentProvider>, context: Context): List<BankApp> =
    getInstalledBankAppsWhichHavePaymentProviders(paymentProviders)
        .map { (installedApp, paymentProvider) ->
            BankApp.fromPaymentProvider(paymentProvider, installedApp, context)
        }

internal fun PackageManager.getInstalledBankAppsWhichHavePaymentProviders(paymentProviders: List<PaymentProvider>): List<Pair<InstalledBankApp, PaymentProvider>> {
    return getInstalledBankApps()
        .mapNotNull { installedApp ->
            // Keep only those installed bank apps which have a corresponding payment provider
            paymentProviders
                .find { provider -> provider.packageName == installedApp.packageName }
                ?.let { paymentProvider ->
                    installedApp to paymentProvider
                }
        }
}

private fun getBankQueryIntent() = Intent().apply {
    action = Intent.ACTION_VIEW
    data = Uri.parse(QueryUri)
}

data class BankApp(
    val name: String,
    val packageName: String,
    val version: String,
    val icon: Drawable?,
    val colors: BankAppColors,
    val paymentProvider: PaymentProvider,
    private val launchIntent: Intent
) {

    fun getIntent(paymentRequestId: String) = Intent(launchIntent).apply {
        data = Uri.parse(getBankUri(paymentRequestId))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BankApp

        if (name != other.name) return false
        if (packageName != other.packageName) return false
        if (version != other.version) return false
        if (icon != other.icon) return false
        if (colors != other.colors) return false
        if (paymentProvider != other.paymentProvider) return false
        if (launchIntent.action != other.launchIntent.action) return false
        if (launchIntent.component != other.launchIntent.component) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + (icon?.hashCode() ?: 0)
        result = 31 * result + colors.hashCode()
        result = 31 * result + paymentProvider.hashCode()
        result = 31 * result + launchIntent.hashCode()
        return result
    }


    companion object {
        internal fun fromPaymentProvider(
            paymentProvider: PaymentProvider,
            installedBankApp: InstalledBankApp,
            context: Context,
        ): BankApp {
            if (paymentProvider.packageName != installedBankApp.packageName) {
                throw IllegalArgumentException(
                    """
                    The payment provider and the installed bank app have different package names:
                        - Payment provider:     ${paymentProvider.packageName}
                        - Installed bank app:   ${installedBankApp.packageName}
                """.trimIndent()
                )
            }
            return BankApp(
                name = paymentProvider.name,
                packageName = paymentProvider.packageName,
                version = installedBankApp.version,
                icon = BitmapFactory.decodeByteArray(paymentProvider.icon, 0, paymentProvider.icon.size)
                    ?.let { bitmap ->
                        BitmapDrawable(context.resources, bitmap)
                    },
                colors = BankAppColors(
                    backgroundColor = Color.parseColor("#${paymentProvider.colors.backgroundColorRGBHex}"),
                    textColor = Color.parseColor("#${paymentProvider.colors.textColoRGBHex}")
                ),
                paymentProvider = paymentProvider,
                launchIntent = installedBankApp.launchIntent
            )
        }
    }
}

data class BankAppColors(
    @ColorInt val backgroundColor: Int,
    @ColorInt val textColor: Int
)

data class InstalledBankApp(
    val packageName: String,
    val version: String,
    val launchIntent: Intent
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InstalledBankApp

        if (packageName != other.packageName) return false
        if (version != other.version) return false
        if (launchIntent.action != other.launchIntent.action) return false
        if (launchIntent.component != other.launchIntent.component) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + launchIntent.hashCode()
        return result
    }

    companion object {
        internal fun fromResolveInfo(resolveInfo: ResolveInfo, packageManager: PackageManager): InstalledBankApp {
            val packageName = resolveInfo.activityInfo.applicationInfo.packageName
            return InstalledBankApp(
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
