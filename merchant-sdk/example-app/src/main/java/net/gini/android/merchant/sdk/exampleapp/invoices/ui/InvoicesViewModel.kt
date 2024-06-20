package net.gini.android.merchant.sdk.exampleapp.invoices.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.gini.android.merchant.sdk.exampleapp.invoices.data.InvoicesRepository
import net.gini.android.merchant.sdk.exampleapp.invoices.ui.model.InvoiceItem
import net.gini.android.merchant.sdk.integratedFlow.MerchantFlowConfiguration
import net.gini.android.merchant.sdk.integratedFlow.MerchantFragment
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import org.slf4j.LoggerFactory

class InvoicesViewModel(
    private val invoicesRepository: InvoicesRepository,
    val paymentComponent: PaymentComponent,
) : ViewModel() {

    val uploadHardcodedInvoicesStateFlow = invoicesRepository.uploadHardcodedInvoicesStateFlow
    val invoicesFlow = invoicesRepository.invoicesFlow.map { invoices ->
        invoices.map { invoice ->
            InvoiceItem.fromInvoice(invoice)
        }
    }

    private val _selectedInvoiceItem: MutableStateFlow<InvoiceItem?> = MutableStateFlow(null)
    val selectedInvoiceItem: StateFlow<InvoiceItem?> = _selectedInvoiceItem

    private val _startIntegratedPaymentFlow = MutableSharedFlow<MerchantFragment>(
        extraBufferCapacity = 1
    )
    val startIntegratedPaymentFlow = _startIntegratedPaymentFlow

    private var merchantFlowConfiguration: MerchantFlowConfiguration? = null

    fun loadInvoicesWithExtractions() {
        viewModelScope.launch {
            async { invoicesRepository.loadInvoicesWithExtractions() }.await()
            invoicesRepository.refreshInvoices()
        }
    }

    fun uploadHardcodedInvoices() {
        viewModelScope.launch {
            invoicesRepository.uploadHardcodedInvoices()
        }
    }

    fun loadPaymentProviderApps() {
        viewModelScope.launch {
            paymentComponent.loadPaymentProviderApps()
        }
    }

    fun setSelectedInvoiceItem(invoiceItem: InvoiceItem) = viewModelScope.launch {
        _selectedInvoiceItem.emit(invoiceItem)
    }

    fun startIntegratedPaymentFlow(documentId: String) {
        _startIntegratedPaymentFlow.tryEmit(paymentComponent.getContainerFragment(documentId, merchantFlowConfiguration))
    }

    fun setIntegratedFlowConfiguration(flowConfiguration: MerchantFlowConfiguration) {
        this.merchantFlowConfiguration = flowConfiguration
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InvoicesViewModel::class.java)

    }
}