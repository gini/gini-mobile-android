package net.gini.android.health.sdk.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.gini.android.core.api.models.Document
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.model.toCommonPaymentDetails
import net.gini.android.health.sdk.review.pager.DocumentPageAdapter
import net.gini.android.health.sdk.util.adjustToLocalDecimalSeparation
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.review.reviewComponent.ReviewComponent
import org.slf4j.LoggerFactory

internal class ReviewViewModel(
    val giniHealth: GiniHealth,
    val configuration: ReviewConfiguration,
    val paymentComponent: PaymentComponent,
    val documentId: String,
    val shouldShowCloseButton: Boolean,
    val reviewFragmentListener: ReviewFragmentListener
) : ViewModel() {

    internal var userPreferences: UserPreferences? = null

    val reviewComponent: ReviewComponent = ReviewComponent(
        paymentComponent = paymentComponent,
        reviewConfig = configuration,
        giniInternalPaymentModule = giniHealth.giniInternalPaymentModule,
        coroutineScope = viewModelScope
    )

    private val _paymentDetails = MutableStateFlow(PaymentDetails("", "", "", ""))
    val paymentDetails: StateFlow<PaymentDetails> = _paymentDetails

    private var _isInfoBarVisible = MutableStateFlow(true)
    var isInfoBarVisible: StateFlow<Boolean> = _isInfoBarVisible

    private val _paymentProviderApp = MutableStateFlow<PaymentProviderApp?>(null)
    val paymentProviderApp: StateFlow<PaymentProviderApp?> = _paymentProviderApp

    val giniInternalPaymentModule = giniHealth.giniInternalPaymentModule

    init {
        viewModelScope.launch {
            giniHealth.paymentFlow.collect { extractedPaymentDetails ->
                if (extractedPaymentDetails is ResultWrapper.Success) {
                    val paymentDetails = paymentDetails.value.overwriteEmptyFields(
                        extractedPaymentDetails.value.copy(
                            amount = extractedPaymentDetails.value.amount.adjustToLocalDecimalSeparation()
                        )
                    )
                    _paymentDetails.value = paymentDetails
                    giniInternalPaymentModule.setPaymentDetails(paymentDetails.toCommonPaymentDetails())
                }
            }
        }
        viewModelScope.launch {
            delay(SHOW_INFO_BAR_MS)
            _isInfoBarVisible.value = false
        }
        viewModelScope.launch {
            paymentComponent.selectedPaymentProviderAppFlow.collect { selectedPaymentProviderAppState ->
                when (selectedPaymentProviderAppState) {
                    is SelectedPaymentProviderAppState.AppSelected -> {
                        _paymentProviderApp.value = selectedPaymentProviderAppState.paymentProviderApp
                    }

                    SelectedPaymentProviderAppState.NothingSelected -> {
                        LOG.error("No selected payment provider app")
                    }
                }
            }
        }
        viewModelScope.launch {
            reviewComponent.paymentDetails.collect { paymentDetails ->
                _paymentDetails.value = _paymentDetails.value.copy(recipient = paymentDetails.recipient, iban = paymentDetails.iban, amount = paymentDetails.amount, purpose = paymentDetails.purpose)
            }
        }
    }

    fun getPages(document: Document): List<DocumentPageAdapter.Page> {
        return (1..document.pageCount).map { pageNumber ->
            DocumentPageAdapter.Page(document.id, pageNumber)
        }
    }

    fun retryDocumentReview() {
        viewModelScope.launch {
            giniHealth.retryDocumentReview()
        }
    }

    fun loadPaymentDetails() {
        viewModelScope.launch {
            giniHealth.setDocumentForReview(documentId)
        }
    }

    class Factory(
        private val giniHealth: GiniHealth,
        private val configuration: ReviewConfiguration,
        private val paymentComponent: PaymentComponent,
        private val documentId: String,
        private val shouldShowCloseButton: Boolean,
        private val reviewFragmentListener: ReviewFragmentListener
    ) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewViewModel(giniHealth, configuration, paymentComponent, documentId, shouldShowCloseButton, reviewFragmentListener) as T
        }
    }

    companion object {
        const val SHOW_INFO_BAR_MS = 3000L
        private val LOG = LoggerFactory.getLogger(ReviewViewModel::class.java)
    }
}

private fun PaymentDetails.overwriteEmptyFields(value: PaymentDetails): PaymentDetails = this.copy(
    recipient = if (recipient.trim().isEmpty()) value.recipient else recipient,
    iban = if (iban.trim().isEmpty()) value.iban else iban,
    amount = if (amount.trim().isEmpty()) value.amount else amount,
    purpose = if (purpose.trim().isEmpty()) value.purpose else purpose,
    extractions = extractions ?: value.extractions,
)
