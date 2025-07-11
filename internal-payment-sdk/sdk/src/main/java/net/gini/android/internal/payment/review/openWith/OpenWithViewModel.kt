package net.gini.android.internal.payment.review.openWith

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.utils.BackListener


internal class OpenWithViewModel private constructor(val paymentComponent: PaymentComponent?, val paymentProviderApp: PaymentProviderApp?, val openWithForwardListener: OpenWithForwardListener?, val backListener: BackListener?, val paymentDetails: PaymentDetails?, val paymentRequestId: String?): ViewModel() {

    private val qrCodeMutableFlow = MutableStateFlow<Bitmap?>(null)
    val qrCodeFlow: StateFlow<Bitmap?> = qrCodeMutableFlow

    suspend fun loadPaymentRequestQrCode() {
        paymentRequestId?.let { id ->
            val qrCode =
                paymentComponent?.paymentModule?.giniHealthAPI?.documentManager?.getPaymentRequestImage(
                    id
                )?.data
            qrCode?.let { byteArray ->
                qrCodeMutableFlow.value = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            }
        }
    }

    class Factory(private val paymentComponent: PaymentComponent?, private val paymentProviderApp: PaymentProviderApp?, private val openWithForwardListener: OpenWithForwardListener?, private val backListener: BackListener?, private val paymentDetails: PaymentDetails?, private val paymentRequestId: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OpenWithViewModel(
                paymentComponent,
                paymentProviderApp,
                openWithForwardListener,
                backListener,
                paymentDetails,
                paymentRequestId
            ) as T
        }
    }

}