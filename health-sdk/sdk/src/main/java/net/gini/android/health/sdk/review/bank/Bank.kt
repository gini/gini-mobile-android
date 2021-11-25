package net.gini.android.health.sdk.review.bank

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources.getSystem
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import net.gini.android.core.api.models.PaymentProvider

internal const val Scheme = "ginipay" // It has to match the scheme in query tag in manifest
private const val PaymentPath = "payment"
internal const val QueryUri = "$Scheme://$PaymentPath/id"
internal fun getBankUri(requestId: String) = "$Scheme://$PaymentPath/$requestId"

fun PackageManager.getInstalledBankApps(): List<InstalledBankApp> = queryIntentActivities(getBankQueryIntent(), 0)
    .map { InstalledBankApp.fromResolveInfo(it, this) }


fun PackageManager.getInstalledPaymentProviderBankApps(paymentProviders: List<PaymentProvider>): List<BankApp> =
    (queryIntentActivities(getBankQueryIntent(), 0) + queryIntentActivities(getBankQueryIntent(), 0)
            + queryIntentActivities(getBankQueryIntent(), 0))
        .map { InstalledBankApp.fromResolveInfo(it, this) }
        .mapNotNull { installedApp ->
            // Keep only those installed bank apps which have a corresponding payment provider
            paymentProviders
                .find { provider -> provider.packageName == installedApp.packageName }
                ?.let { paymentProvider ->
                    BankApp.fromPaymentProvider(paymentProvider, installedApp)
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
    val icon: Drawable,
    val colors: BankAppColors,
    val paymentProvider: PaymentProvider,
    private val launchIntent: Intent
) {

    fun getIntent(paymentRequestId: String) = Intent(launchIntent).apply {
        data = Uri.parse(getBankUri(paymentRequestId))
    }

    companion object {
        internal fun fromPaymentProvider(
            paymentProvider: PaymentProvider,
            installedBankApp: InstalledBankApp
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
                // TODO: use paymentProvider.icon
                icon = ResourcesCompat.getDrawable(getSystem(), android.R.drawable.ic_dialog_email, null)!!,
                // TODO: use paymentProvider.colors
                colors = BankAppColors(
                    backgroundColor = Color.MAGENTA,
                    textColor = Color.WHITE
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
