package net.gini.android.merchant.sdk.exampleapp.invoices.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.exampleapp.invoices.data.InvoicesRepository
import net.gini.android.merchant.sdk.exampleapp.invoices.ui.model.InvoiceItem
import net.gini.android.merchant.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.merchant.sdk.integratedFlow.PaymentFlowFragment
import org.slf4j.LoggerFactory

class InvoicesViewModel(
    private val invoicesRepository: InvoicesRepository,
    val giniMerchant: GiniMerchant
) : ViewModel() {

    val uploadHardcodedInvoicesStateFlow = invoicesRepository.uploadHardcodedInvoicesStateFlow
    val invoicesFlow = invoicesRepository.invoicesFlow.map { invoices ->
        invoices.map { invoice ->
            InvoiceItem.fromInvoice(invoice)
        }
    }

    private val _selectedInvoiceItem: MutableStateFlow<InvoiceItem?> = MutableStateFlow(null)
    val selectedInvoiceItem: StateFlow<InvoiceItem?> = _selectedInvoiceItem

    private val _startIntegratedPaymentFlow = MutableSharedFlow<PaymentFlowFragment>(
        extraBufferCapacity = 1
    )
    val startIntegratedPaymentFlow = _startIntegratedPaymentFlow

    private var paymentFlowConfiguration: PaymentFlowConfiguration? = null

    private var _finishPaymentFlow = MutableStateFlow<Boolean?>(null)
    val finishPaymentFlow: StateFlow<Boolean?> = _finishPaymentFlow

    fun startObservingPaymentFlow() = viewModelScope.launch {
        giniMerchant.eventsFlow.collect { event ->
            when (event) {
                is GiniMerchant.MerchantSDKEvents.OnFinishedWithPaymentRequestCreated,
                is GiniMerchant.MerchantSDKEvents.OnFinishedWithCancellation -> {
                    _finishPaymentFlow.tryEmit(true)
                }
                else -> {}
            }
        }
    }

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
            giniMerchant.loadPaymentProviderApps()
        }
    }

    fun setSelectedInvoiceItem(invoiceItem: InvoiceItem) = viewModelScope.launch {
        _selectedInvoiceItem.emit(invoiceItem)
    }

    fun startIntegratedPaymentFlow(documentId: String) {
        _startIntegratedPaymentFlow.tryEmit(giniMerchant.getContainerFragment(documentId, paymentFlowConfiguration))
    }

    fun setIntegratedFlowConfiguration(flowConfiguration: PaymentFlowConfiguration) {
        this.paymentFlowConfiguration = flowConfiguration
    }

    fun resetFinishPaymentFlow() {
        _finishPaymentFlow.tryEmit(null)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InvoicesViewModel::class.java)

    }
}