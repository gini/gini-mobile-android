package net.gini.android.capture.di

import net.gini.android.capture.einvoice.GetEInvoiceFeatureEnabledUseCase
import net.gini.android.capture.wero.GetWeroQRCodeScanningEnabledUseCase

object CaptureSdkJavaInterop {

    @JvmStatic
    fun getEInvoiceFeatureEnabledUseCase(): GetEInvoiceFeatureEnabledUseCase {
        return getGiniCaptureKoin().get()
    }

    @JvmStatic
    fun getWeroQRCodeScanningEnabledUseCase(): GetWeroQRCodeScanningEnabledUseCase {
        return getGiniCaptureKoin().get()
    }

}
