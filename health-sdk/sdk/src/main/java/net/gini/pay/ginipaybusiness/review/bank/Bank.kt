package net.gini.pay.ginipaybusiness.review.bank

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri

internal const val Scheme = "ginipay" // It has to match the scheme in query tag in manifest
private const val PaymentPath = "payment"
internal const val QueryUri = "$Scheme://$PaymentPath/id"
internal fun getBankUri(requestId: String) = "$Scheme://$PaymentPath/$requestId"

fun PackageManager.getBanks(): List<BankApp> = queryIntentActivities(getBankQueryIntent(), 0).map { app ->
    BankApp(packageManager = this, resolveInfo = app)
}

fun getBankQueryIntent() = Intent().apply {
    action = Intent.ACTION_VIEW
    data = Uri.parse(QueryUri)
}

class BankApp(
    packageManager: PackageManager,
    private val resolveInfo: ResolveInfo,
) {

    val name: String = resolveInfo.loadLabel(packageManager).toString()
    val packageName: String = resolveInfo.activityInfo.applicationInfo.packageName
    val version: String = packageManager.getPackageInfo(packageName, 0).versionName
    private val component: ComponentName = ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)

    fun getIconDrawable(packageManager: PackageManager): Drawable = resolveInfo.loadIcon(packageManager)

    fun getIntent(paymentRequestId: String) = Intent().apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse(getBankUri(paymentRequestId))
        component = this@BankApp.component
    }
}