package net.gini.android.health.sdk.exampleapp.invoices.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.exampleapp.invoices.data.InvoicesRepository
import net.gini.android.health.sdk.exampleapp.invoices.data.model.DocumentWithExtractions
import net.gini.android.health.sdk.exampleapp.invoices.ui.model.InvoiceItem
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.Currency
import java.util.Locale

class InvoicesViewModel(
    private val invoicesRepository: InvoicesRepository
) : ViewModel() {

    val uploadHardcodedInvoicesState = invoicesRepository.uploadHardcodedInvoicesState
    val invoicesFlow = invoicesRepository.invoicesFlow.map { invoices ->
        invoices.map { invoice ->
            InvoiceItem.fromInvoice(invoice)
        }
    }

    fun loadInvoicesWithExtractions() {
        viewModelScope.launch {
            invoicesRepository.loadInvoicesWithExtractions()
        }
    }

    fun uploadHardcodedInvoices() {
        viewModelScope.launch {
            invoicesRepository.uploadHardcodedInvoices()
        }
    }
}