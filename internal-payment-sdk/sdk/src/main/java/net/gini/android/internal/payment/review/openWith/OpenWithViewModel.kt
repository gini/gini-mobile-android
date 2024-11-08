package net.gini.android.internal.payment.review.openWith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.utils.BackListener

internal class OpenWithViewModel private constructor(val paymentComponent: PaymentComponent?, val paymentProviderApp: PaymentProviderApp?, val openWithForwardListener: OpenWithForwardListener?, val backListener: BackListener?, val paymentDetails: PaymentDetails?): ViewModel() {
    class Factory(private val paymentComponent: PaymentComponent?, private val paymentProviderApp: PaymentProviderApp?, private val openWithForwardListener: OpenWithForwardListener?, private val backListener: BackListener?, private val paymentDetails: PaymentDetails?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OpenWithViewModel(paymentComponent, paymentProviderApp, openWithForwardListener, backListener, paymentDetails) as T
        }
    }

}