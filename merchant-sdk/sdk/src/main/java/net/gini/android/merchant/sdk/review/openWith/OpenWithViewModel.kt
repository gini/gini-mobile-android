package net.gini.android.merchant.sdk.review.openWith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.util.BackListener

internal class OpenWithViewModel private constructor(val paymentProviderApp: PaymentProviderApp?, val backListener: BackListener?): ViewModel() {
    class Factory(private val paymentProviderApp: PaymentProviderApp?, private val backListener: BackListener?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OpenWithViewModel(paymentProviderApp, backListener) as T
        }
    }

}