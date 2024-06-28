package net.gini.android.merchant.sdk.util

import net.gini.android.merchant.sdk.api.payment.model.PaymentRequest
import java.io.File

internal sealed class PaymentNextStep {
    object RedirectToBank : PaymentNextStep()
    object ShowOpenWithSheet : PaymentNextStep()
    object ShowInstallApp : PaymentNextStep()
    data class OpenSharePdf(val file: File, val paymentRequest: PaymentRequest) : PaymentNextStep()
    data class SetLoadingVisibility(val isVisible: Boolean) : PaymentNextStep()
}