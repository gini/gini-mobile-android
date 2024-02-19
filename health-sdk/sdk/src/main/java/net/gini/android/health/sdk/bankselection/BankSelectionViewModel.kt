package net.gini.android.health.sdk.bankselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent

internal class BankSelectionViewModel(val paymentComponent: PaymentComponent?) : ViewModel() {

    class Factory(private val paymentComponent: PaymentComponent?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BankSelectionViewModel(paymentComponent) as T
        }
    }
}