package net.gini.android.models

import net.gini.android.response.PaymentProviderResponse

/**
 * A payment provider is a Gini partner which integrated the GiniPay for Banks SDK into their mobile apps.
 */
data class PaymentProvider(
    val id: String,
    val name: String,
    /**
     * Package name of the bank app that corresponds to this provider.
     */
    val packageName: String,
    /**
     * The minimal required app versions per platform
     */
    val appVersion: String,
)

internal fun PaymentProviderResponse.toPaymentProvider() = PaymentProvider(
    id, name, packageNameAndroid, minAppVersion.android
)