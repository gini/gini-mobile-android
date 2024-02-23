package net.gini.android.health.sdk.exampleapp.invoices.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.exampleapp.invoices.data.InvoicesRepository
import net.gini.android.health.sdk.exampleapp.invoices.ui.model.InvoiceItem
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.review.ReviewConfiguration
import net.gini.android.health.sdk.review.ReviewFragment
import java.lang.IllegalStateException

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

    val _paymentReviewFragmentFlow = MutableStateFlow<PaymentReviewFragmentState>(PaymentReviewFragmentState.Idle)
    val paymentReviewFragmentStateFlow = _paymentReviewFragmentFlow.asStateFlow()

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

    fun loadPaymentProviderApps() {
        viewModelScope.launch {
            paymentComponent.loadPaymentProviderApps()
        }
    }

    fun getPaymentReviewFragment(paymentComponentViewIdentifier: String) {
        viewModelScope.launch {
            Log.d(
                InvoicesViewModel::class.simpleName,
                "Getting payment review fragment for id: $paymentComponentViewIdentifier"
            )

            _paymentReviewFragmentFlow.value = PaymentReviewFragmentState.Loading

            val documentWithExtractions =
                invoicesRepository.invoicesFlow.value.find { it.documentId == paymentComponentViewIdentifier }

            if (documentWithExtractions != null) {
                try {
                    val paymentReviewFragment = paymentComponent.getPaymentReviewFragment(
                        documentWithExtractions.documentId,
                        ReviewConfiguration(showCloseButton = true)
                    )
                    _paymentReviewFragmentFlow.value = PaymentReviewFragmentState.Success(paymentReviewFragment)
                } catch (e: Exception) {
                    Log.e(InvoicesViewModel::class.simpleName, "Error getting payment review fragment", e)
                    _paymentReviewFragmentFlow.value = PaymentReviewFragmentState.Error(e)
                }
            } else {
                Log.e(InvoicesViewModel::class.simpleName, "Document with id $paymentComponentViewIdentifier not found")
                _paymentReviewFragmentFlow.value = PaymentReviewFragmentState.Error(IllegalStateException("Document with id $paymentComponentViewIdentifier not found"))
            }
            _paymentReviewFragmentFlow.emit(PaymentReviewFragmentState.Idle)
        }
    }
}

sealed class PaymentReviewFragmentState {
    object Loading : PaymentReviewFragmentState()
    data class Success(val fragment: ReviewFragment) : PaymentReviewFragmentState()
    data class Error(val throwable: Throwable) : PaymentReviewFragmentState()
    object Idle : PaymentReviewFragmentState()
}