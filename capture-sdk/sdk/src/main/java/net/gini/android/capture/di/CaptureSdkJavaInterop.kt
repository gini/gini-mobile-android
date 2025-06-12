package net.gini.android.capture.di

import net.gini.android.capture.einvoice.GetEInvoiceFeatureEnabledUseCase

object CaptureSdkJavaInterop {

    @JvmStatic
    fun getEInvoiceFeatureEnabledUseCase(): GetEInvoiceFeatureEnabledUseCase {
        return getGiniCaptureKoin().get()
    }

}
