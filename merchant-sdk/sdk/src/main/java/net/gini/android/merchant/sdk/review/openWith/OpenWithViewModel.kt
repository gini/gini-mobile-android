package net.gini.android.merchant.sdk.review.openWith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp

internal class OpenWithViewModel private constructor(val paymentProviderApp: PaymentProviderApp?): ViewModel() {
    class Factory(private val paymentProviderApp: PaymentProviderApp?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OpenWithViewModel(paymentProviderApp) as T
        }
    }

}