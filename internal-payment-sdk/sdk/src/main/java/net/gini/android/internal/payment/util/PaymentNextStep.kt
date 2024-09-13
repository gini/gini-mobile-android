package net.gini.android.internal.payment.util

import java.io.File

sealed class PaymentNextStep {
    object RedirectToBank : PaymentNextStep()
    object ShowOpenWithSheet : PaymentNextStep()
    object ShowInstallApp : PaymentNextStep()
    data class OpenSharePdf(val file: File) : PaymentNextStep()
    data class SetLoadingVisibility(val isVisible: Boolean) : PaymentNextStep()
}