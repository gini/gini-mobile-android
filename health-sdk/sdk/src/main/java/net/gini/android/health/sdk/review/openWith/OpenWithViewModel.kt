package net.gini.android.health.sdk.review.openWith

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp

internal class OpenWithViewModel private constructor(val paymentProviderApp: PaymentProviderApp?): ViewModel() {
    private  val context: Context? = null

    class Factory(private val paymentProviderApp: PaymentProviderApp?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OpenWithViewModel(paymentProviderApp) as T
        }
    }


}