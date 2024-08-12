package net.gini.android.health.sdk.exampleapp.invoices.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.exampleapp.invoices.data.InvoicesRepository
import net.gini.android.health.sdk.exampleapp.invoices.ui.model.InvoiceItem
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentComponentConfiguration
import net.gini.android.health.sdk.review.ReviewConfiguration
import net.gini.android.health.sdk.review.ReviewFragment
import net.gini.android.health.sdk.review.model.ResultWrapper
import org.slf4j.LoggerFactory

class InvoicesViewModel(
    private val invoicesRepository: InvoicesRepository,
    val paymentComponent: PaymentComponent
) : ViewModel() {

    val uploadHardcodedInvoicesStateFlow = invoicesRepository.uploadHardcodedInvoicesStateFlow
    val invoicesFlow = invoicesRepository.invoicesFlow.map { invoices ->
        invoices.map { invoice ->
            InvoiceItem.fromInvoice(invoice)
        }
    }
    val paymentProviderAppsFlow = paymentComponent.paymentProviderAppsFlow

    val openBankState = invoicesRepository.giniHealth.openBankState

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
            paymentComponent.loadPaymentProviderApps()
        }
    }

    fun getPaymentReviewFragment(documentId: String): Result<ReviewFragment> {
        val documentWithExtractions =
            invoicesRepository.invoicesFlow.value.find { it.documentId == documentId }

        return if (documentWithExtractions != null) {
            return try {
                val paymentReviewFragment = paymentComponent.getPaymentReviewFragment(
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

    fun setPaymentComponentConfig(paymentComponentConfiguration: PaymentComponentConfiguration) {
        paymentComponent.paymentComponentConfiguration = paymentComponentConfiguration
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InvoicesViewModel::class.java)

    }
}
