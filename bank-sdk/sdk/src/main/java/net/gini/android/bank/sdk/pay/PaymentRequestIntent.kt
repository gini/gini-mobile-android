package net.gini.android.bank.sdk.pay

import android.content.Intent
import android.net.Uri
import net.gini.android.core.api.models.ResolvedPayment

internal const val Scheme = "ginipay" // It has to match the scheme in query tag in manifest
private const val PaymentPath = "payment"

fun getRequestId(intent: Intent): String {
    check(intent.action == Intent.ACTION_VIEW) { "Intent has wrong action" }
    val uri = intent.data
    check(uri != null) { "Intent has wrong action" }
    check(uri.scheme == Scheme) { "Intent has wrong scheme" }
    val path = uri.pathSegments
    check(path != null && path.size == 1 && uri.authority == PaymentPath) { "Intent has wrong path" }
    return path[0]
}

fun ResolvedPayment.getBusinessIntent() = Intent().apply {
    action = Intent.ACTION_VIEW
    data = Uri.parse(this@getBusinessIntent.requesterUri)
}