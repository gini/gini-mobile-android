package net.gini.android.health.sdk.exampleapp.invoices.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.invoices.data.InvoicesRepository
import net.gini.android.health.sdk.exampleapp.invoices.ui.model.InvoiceItem
import net.gini.android.health.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.health.sdk.integratedFlow.PaymentFragment
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.internal.payment.paymentComponent.PaymentComponentConfiguration
import net.gini.android.internal.payment.review.ReviewConfiguration
import org.slf4j.LoggerFactory

class InvoicesViewModel(
    private val invoicesRepository: InvoicesRepository,
    private val giniHealth: GiniHealth
) : ViewModel() {

    val uploadHardcodedInvoicesStateFlow = invoicesRepository.uploadHardcodedInvoicesStateFlow
    val invoicesFlow = invoicesRepository.invoicesFlow.map { invoices ->
        invoices.map { invoice ->
            InvoiceItem.fromInvoice(invoice)
        }
    }
    val giniPaymentModule = giniHealth.giniInternalPaymentModule
    val paymentProviderAppsFlow = giniPaymentModule.paymentComponent.paymentProviderAppsFlow

    val openBankState = invoicesRepository.giniHealth.openBankState
    private val _startIntegratedPaymentFlow = MutableSharedFlow<PaymentDetails>(
        extraBufferCapacity = 1
    )
    val startIntegratedPaymentFlow = _startIntegratedPaymentFlow

    fun updateDocument() {
        viewModelScope.launch {
            with(invoicesRepository) {
                requestDocumentExtractionAndSaveToLocal((giniHealth.documentFlow.value as ResultWrapper.Success).value)
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
            giniPaymentModule.paymentComponent.loadPaymentProviderApps()
        }
    }

    fun getPaymentReviewFragment(documentId: String?): Result<PaymentFragment> {
        val documentWithExtractions =
            invoicesRepository.invoicesFlow.value.find { it.documentId == documentId }

        return if (documentWithExtractions != null) {
            return try {
                val paymentReviewFragment = invoicesRepository.giniHealth.getPaymentFragmentWithDocument(
                    documentWithExtractions.documentId,
                    ReviewConfiguration(showCloseButton = true)
                )
                Result.success(paymentReviewFragment)
            } catch (e: Exception) {
                LOG.error("Error getting payment review fragment", e)
                Result.failure(e)
            }
        } else {
            LOG.error("Document with id {} not found", documentId)
            Result.failure(IllegalStateException("Document with id $documentId not found"))
        }
    }

    fun startPaymentFlowWithoutDocument(paymentDetails: PaymentDetails){
        _startIntegratedPaymentFlow.tryEmit(paymentDetails)
    }

    fun getPaymentFragmentForPaymentDetails(paymentDetails: PaymentDetails): Result<PaymentFragment> {
        try {
            val paymentFragment = invoicesRepository.giniHealth.getPaymentFragmentWithoutDocument(paymentDetails, PaymentFlowConfiguration(shouldShowReviewFragment = false, shouldHandleErrorsInternally = true))
            return Result.success(paymentFragment)
        } catch (e: Exception) {
            LOG.error("Error getting payment fragment without document", e)
            return Result.failure(e)
        }
    }

    fun setPaymentComponentConfig(paymentComponentConfiguration: PaymentComponentConfiguration) {
        giniPaymentModule.paymentComponent.paymentComponentConfiguration = paymentComponentConfiguration
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InvoicesViewModel::class.java)

    }
}
