package net.gini.android.merchant.sdk.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.gini.android.core.api.Resource
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.api.payment.model.PaymentRequest
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.review.openWith.OpenWithPreferences
import net.gini.android.merchant.sdk.review.reviewComponent.ReviewComponent.Companion.SHOW_OPEN_WITH_TIMES
import net.gini.android.merchant.sdk.util.extensions.createTempPdfFile
import java.io.File


internal interface FlowBottomSheetsManager {
    var openWithPreferences: OpenWithPreferences?
    var openWithCounter: Int
    val paymentNextStepFlow: MutableSharedFlow<PaymentNextStep>
    val paymentRequestFlow: MutableStateFlow<PaymentRequest?>
    val shareWithFlowStarted: MutableStateFlow<Boolean>

    fun startObservingOpenWithCount(coroutineScope: CoroutineScope, paymentProviderAppId: String) {
        coroutineScope.launch {
            openWithPreferences?.getLiveCountForPaymentProviderId(paymentProviderAppId)
                ?.collectLatest {
                    openWithCounter = it ?: 0
                }
        }
    }

    fun incrementOpenWithCounter(coroutineScope: CoroutineScope, paymentProviderAppId: String) {
        coroutineScope.launch {
            openWithPreferences?.incrementCountForPaymentProviderId(paymentProviderAppId)
        }
    }

    private fun getFileAsByteArray(externalCacheDir: File?, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                sendFeedback()
                val paymentRequest = try {
                    getPaymentRequest()
                } catch (throwable: Throwable) {
                    emitSDKEvent(GiniMerchant.PaymentState.Error(throwable))
                    return@withContext
                }
                if (paymentRequest == null) {
                    emitSDKEvent(GiniMerchant.PaymentState.Error(Exception("Payment request is null")))
                    return@withContext
                }
                val byteArrayResource = async {
                    getPaymentRequestDocument(paymentRequest)
                }.await()
                when (byteArrayResource) {
                    is Resource.Cancelled -> {
                        emitSDKEvent(GiniMerchant.PaymentState.Error(Exception("Cancelled")))
                    }
                    is Resource.Error -> {
                        emitSDKEvent(GiniMerchant.PaymentState.Error(byteArrayResource.exception ?: Exception("Error")))
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

    fun emitSDKEvent(sdkEvent: GiniMerchant.PaymentState)
    fun sendFeedback()
    suspend fun getPaymentRequest(): PaymentRequest?
    suspend fun getPaymentRequestDocument(paymentRequest: PaymentRequest): Resource<ByteArray>

}