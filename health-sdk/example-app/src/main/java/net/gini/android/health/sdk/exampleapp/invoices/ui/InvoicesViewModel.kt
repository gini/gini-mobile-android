package net.gini.android.health.sdk.exampleapp.invoices.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.exampleapp.invoices.data.InvoicesRepository

class InvoicesViewModel(
    private val invoicesRepository: InvoicesRepository
): ViewModel() {

    val uploadHardcodedInvoicesState = invoicesRepository.uploadHardcodedInvoicesState
    val invoicesFlow = invoicesRepository.invoicesFlow

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