package net.gini.android.internal.payment.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.gini.android.core.api.Resource
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.api.model.PaymentRequest
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.review.reviewComponent.ReviewComponent.Companion.SHOW_OPEN_WITH_TIMES
import java.io.File


interface FlowBottomSheetsManager {
    val giniInternalPaymentModule: GiniInternalPaymentModule?
    var openWithCounter: Int
    val paymentNextStepFlow: MutableSharedFlow<PaymentNextStep>
    val paymentRequestFlow: MutableStateFlow<PaymentRequest?>
    val shareWithFlowStarted: MutableStateFlow<Boolean>

    fun startObservingOpenWithCount(coroutineScope: CoroutineScope, paymentProviderAppId: String) {
        coroutineScope.launch {
            giniInternalPaymentModule?.getLiveCountForPaymentProviderId(paymentProviderAppId)
                ?.collectLatest {
                    openWithCounter = it ?: 0
                }
        }
    }

    fun incrementOpenWithCounter(coroutineScope: CoroutineScope, paymentProviderAppId: String) {
        coroutineScope.launch {
            giniInternalPaymentModule?.incrementCountForPaymentProviderId(paymentProviderAppId)
        }
    }

    private fun getFileAsByteArray(externalCacheDir: File?, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val paymentRequest = try {
                    getPaymentRequest()
                } catch (throwable: Throwable) {
                    giniInternalPaymentModule?.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnErrorOccurred(throwable))
                    return@withContext
                }
                if (paymentRequest == null) {
                    giniInternalPaymentModule?.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnErrorOccurred(Exception("Payment request is null")))
                    return@withContext
                }
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
                            paymentRequestFlow.value = paymentRequest
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
        if (openWithCounter < SHOW_OPEN_WITH_TIMES) {
            emitPaymentNextStep(PaymentNextStep.ShowOpenWithSheet)
        } else {
            emitPaymentNextStep(PaymentNextStep.SetLoadingVisibility(true))
            getFileAsByteArray(externalCacheDir, coroutineScope)
        }
    }

    fun sharePdf(paymentProviderApp: PaymentProviderApp?, externalCacheDir: File?, coroutineScope: CoroutineScope) {
        paymentNextStepFlow.tryEmit(PaymentNextStep.SetLoadingVisibility(true))
        getFileAsByteArray(externalCacheDir, coroutineScope)
    }


    private fun emitPaymentNextStep(paymentNextStep: PaymentNextStep) {
        paymentNextStepFlow.tryEmit(paymentNextStep)
    }

    suspend fun getPaymentRequest(): PaymentRequest?
    suspend fun getPaymentRequestDocument(paymentRequest: PaymentRequest): Resource<ByteArray>

}
