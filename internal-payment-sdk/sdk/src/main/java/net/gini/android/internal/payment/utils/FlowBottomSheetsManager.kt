package net.gini.android.internal.payment.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.gini.android.core.api.Resource
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.api.model.PaymentRequest
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import java.io.File


interface FlowBottomSheetsManager {
    val giniInternalPaymentModule: GiniInternalPaymentModule?
    val paymentNextStepFlow: MutableSharedFlow<PaymentNextStep>
    val paymentRequestFlow: MutableStateFlow<PaymentRequest?>
    val shareWithFlowStarted: MutableStateFlow<Boolean>

    private fun getFileAsByteArray(externalCacheDir: File?, coroutineScope: CoroutineScope, paymentRequest: PaymentRequest?) {
        if (paymentRequest == null) {
            giniInternalPaymentModule?.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnErrorOccurred(Exception("Payment request is null")))
            return
        }
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val byteArrayResource = async {
                    getPaymentRequestDocument(paymentRequest)
                }.await()
                when (byteArrayResource) {
                    is Resource.Cancelled -> {
                        giniInternalPaymentModule?.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnErrorOccurred(Exception("Cancelled")))
                    }
                    is Resource.Error -> {
                        giniInternalPaymentModule?.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnErrorOccurred(byteArrayResource.exception ?: Exception("Error")))
                    }
                    is Resource.Success -> {
                        val newFile = externalCacheDir?.createTempPdfFile(byteArrayResource.data, "payment-request")
                        newFile?.let {
                            emitPaymentNextStep(PaymentNextStep.OpenSharePdf(file = it))
                        }
                    }
                }
            }
        }
    }

    fun checkNextStep(paymentProviderApp: PaymentProviderApp?, externalCacheDir: File?, coroutineScope: CoroutineScope) {
        if (paymentProviderApp?.paymentProvider?.gpcSupported() == true) {
            if (paymentProviderApp.isInstalled()) emitPaymentNextStep(PaymentNextStep.RedirectToBank)
            else emitPaymentNextStep(PaymentNextStep.ShowInstallApp)
            return
        }

        coroutineScope.launch { getPaymentRequestForOpenWith() }
    }

    private suspend fun getPaymentRequestForOpenWith() {
        emitPaymentNextStep(PaymentNextStep.SetLoadingVisibility(true))
        val paymentRequest = try {
            getPaymentRequest()
        } catch (throwable: Throwable) {
            emitPaymentNextStep(PaymentNextStep.SetLoadingVisibility(false))
            giniInternalPaymentModule?.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnErrorOccurred(throwable))
            return
        }
        if (paymentRequest == null) {
            emitPaymentNextStep(PaymentNextStep.SetLoadingVisibility(false))
            giniInternalPaymentModule?.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnErrorOccurred(Exception("Payment request is null")))
            return
        }

        emitPaymentNextStep(PaymentNextStep.SetLoadingVisibility(false))
        paymentRequestFlow.value = paymentRequest
        emitPaymentNextStep(PaymentNextStep.ShowOpenWithSheet)
    }

    fun sharePdf(paymentProviderApp: PaymentProviderApp?, externalCacheDir: File?, coroutineScope: CoroutineScope, paymentRequest: PaymentRequest?) {
        paymentNextStepFlow.tryEmit(PaymentNextStep.SetLoadingVisibility(true))
        getFileAsByteArray(externalCacheDir, coroutineScope, paymentRequest)
    }


    private fun emitPaymentNextStep(paymentNextStep: PaymentNextStep) {
        paymentNextStepFlow.tryEmit(paymentNextStep)
    }

    suspend fun getPaymentRequest(): PaymentRequest?
    suspend fun getPaymentRequestDocument(paymentRequest: PaymentRequest): Resource<ByteArray>

    suspend fun getPaymentRequestImage(paymentRequest: PaymentRequest): Resource<ByteArray>

}
