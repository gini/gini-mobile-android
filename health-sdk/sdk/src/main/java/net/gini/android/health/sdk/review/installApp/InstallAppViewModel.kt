package net.gini.android.health.sdk.review.installApp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import org.slf4j.LoggerFactory

internal class InstallAppViewModel(val paymentProviderApp: PaymentProviderApp?) : ViewModel() {
    class Factory(private val paymentProviderApp: PaymentProviderApp?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InstallAppViewModel(paymentProviderApp) as T
        }
    }
}