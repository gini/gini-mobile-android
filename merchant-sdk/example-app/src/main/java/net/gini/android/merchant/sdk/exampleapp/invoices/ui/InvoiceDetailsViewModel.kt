package net.gini.android.merchant.sdk.exampleapp.invoices.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.gini.android.merchant.sdk.exampleapp.invoices.ui.model.InvoiceItem


class InvoiceDetailsViewModel(val invoiceItem: InvoiceItem?) : ViewModel() {

    class Factory(private val invoiceItem: InvoiceItem?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InvoiceDetailsViewModel(invoiceItem) as T
        }
    }

}